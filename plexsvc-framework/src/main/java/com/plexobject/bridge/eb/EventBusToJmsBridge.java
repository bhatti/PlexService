package com.plexobject.bridge.eb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.plexobject.bus.EventBus;
import com.plexobject.bus.impl.EventBusImpl;
import com.plexobject.domain.Constants;
import com.plexobject.encode.ObjectCodecFactory;
import com.plexobject.encode.json.JsonObjectCodec;
import com.plexobject.handler.AbstractResponseDispatcher;
import com.plexobject.handler.Request;
import com.plexobject.handler.RequestHandler;
import com.plexobject.jms.JmsClient;
import com.plexobject.jms.JmsResponseDispatcher;
import com.plexobject.service.Lifecycle;
import com.plexobject.service.ServiceConfig.Method;
import com.plexobject.service.ServiceConfig.Protocol;
import com.plexobject.util.Configuration;
import com.plexobject.util.IOUtils;

public class EventBusToJmsBridge implements Lifecycle {
	private static final Logger log = LoggerFactory
			.getLogger(EventBusToJmsBridge.class);

	static class EBListener implements RequestHandler, Lifecycle {
		private final JmsClient jmsClient;
		private final EventBus eb;
		private final EventBusToJmsEntry entry;
		private long subscriptionId;

		private EBListener(JmsClient jmsClient, EventBus eb,
				EventBusToJmsEntry entry) {
			this.jmsClient = jmsClient;
			this.eb = eb;
			this.entry = entry;
		}

		@Override
		public void handle(Request request) {
			Map<String, Object> params = new HashMap<>();
			params.putAll(request.getProperties());
			params.putAll(request.getHeaders());
			try {
				String payload = ObjectCodecFactory.getInstance()
						.getObjectCodec(entry.getCodecType())
						.encode(request.getPayload());
				jmsClient.send(entry.getTarget(), params, payload);
				log.info("Forwarding " + entry + "'s message " + payload);

			} catch (Exception e) {
				log.error("Failed to send request", e);
			}
		}

		@Override
		public synchronized void start() {
			subscriptionId = this.eb.subscribe(entry.getSource(), this, null);
		}

		@Override
		public synchronized void stop() {
			this.eb.unsubscribe(subscriptionId);
			subscriptionId = -1;
		}

		@Override
		public synchronized boolean isRunning() {
			return subscriptionId >= 0;
		}
	}

	static class JmsListener implements MessageListener, ExceptionListener,
			Lifecycle {
		private final JmsClient jmsClient;
		private final EventBus eb;
		private final EventBusToJmsEntry entry;
		private MessageConsumer consumer;

		JmsListener(JmsClient jmsClient, EventBus eb, EventBusToJmsEntry entry) {
			this.jmsClient = jmsClient;
			this.eb = eb;
			this.entry = entry;
		}

		@Override
		public void onMessage(Message message) {
			TextMessage txtMessage = (TextMessage) message;
			try {
				Map<String, Object> params = JmsClient.getParams(message);
				String sessionId = (String) params.get(Constants.SESSION_ID);

				final String textPayload = txtMessage.getText();
				Object payload = ObjectCodecFactory
						.getInstance()
						.getObjectCodec(entry.getCodecType())
						.decode(textPayload, entry.getRequestTypeClass(),
								params);
				AbstractResponseDispatcher dispatcher = message.getJMSReplyTo() != null ? new JmsResponseDispatcher(
						jmsClient, message.getJMSReplyTo()) : null;
				if (dispatcher != null) {
					dispatcher.setCodecType(entry.getCodecType());
				}
				Request req = Request.builder().setProtocol(Protocol.JMS)
						.setMethod(Method.MESSAGE).setProperties(params)
						.setPayload(payload).setSessionId(sessionId)
						.setResponseDispatcher(dispatcher).build();
				log.info("Forwarding " + entry + "'s message " + req);
				eb.publish(entry.getTarget(), req);
			} catch (Exception e) {
				log.error("Failed to handle request", e);
			}
		}

		@Override
		public void onException(JMSException ex) {
			log.error("Found error while listening, will resubscribe", ex);
			try {
				stop();
				start();
			} catch (Exception e) {
				log.error("Failed to resubscribe", e);
			}
		}

		@Override
		public synchronized void start() {
			try {
				this.consumer = jmsClient.createConsumer(entry.getSource());
				consumer.setMessageListener(this);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public synchronized void stop() {
			try {
				this.consumer.close();
			} catch (Exception e) {
			}
			this.consumer = null;
		}

		@Override
		public synchronized boolean isRunning() {
			return this.consumer != null;
		}
	}

	private boolean running;
	private final JmsClient jmsClient;
	private final EventBus eb;
	private final Map<EventBusToJmsEntry, EBListener> ebListeners = new ConcurrentHashMap<>();
	private final Map<EventBusToJmsEntry, JmsListener> jmsListeners = new ConcurrentHashMap<>();

	public EventBusToJmsBridge(JmsClient jmsClient,
			Collection<EventBusToJmsEntry> entries, EventBus eb)
			throws JMSException {
		this.jmsClient = jmsClient;
		this.eb = eb;

		for (EventBusToJmsEntry e : entries) {
			add(e);
		}
	}

	public EBListener getEBListener(EventBusToJmsEntry e) {
		return ebListeners.get(e);
	}

	public JmsListener getJmsListener(EventBusToJmsEntry e) {
		return jmsListeners.get(e);
	}

	public synchronized void add(EventBusToJmsEntry e) {
		if (e.getType() == EventBusToJmsEntry.Type.EB_CHANNEL_TO_JMS) {
			EBListener listener = new EBListener(jmsClient, eb, e);
			ebListeners.put(e, listener);
		} else {
			JmsListener listener = new JmsListener(jmsClient, eb, e);
			jmsListeners.put(e, listener);
		}
		log.info("Adding " + e);
	}

	public synchronized void remove(EventBusToJmsEntry e) {
		if (e.getType() == EventBusToJmsEntry.Type.EB_CHANNEL_TO_JMS) {
			EBListener listener = ebListeners.remove(e);
			if (listener != null) {
				listener.stop();
			}
		} else {
			JmsListener listener = jmsListeners.remove(e);
			if (listener != null) {
				listener.stop();
			}
		}
	}

	public static void run(Configuration config,
			Collection<EventBusToJmsEntry> entries) throws JMSException {
		EventBus eb = new EventBusImpl();
		JmsClient jmsClient = new JmsClient(config);
		EventBusToJmsBridge bridge = new EventBusToJmsBridge(jmsClient,
				entries, eb);
		bridge.start();
	}

	@Override
	public synchronized void start() {
		if (running) {
			return;
		}
		running = true;
		jmsClient.start();
		for (EBListener l : ebListeners.values()) {
			l.start();
		}
		for (JmsListener l : jmsListeners.values()) {
			l.start();
		}
	}

	@Override
	public synchronized void stop() {
		if (!running) {
			return;
		}
		running = false;
		jmsClient.stop();
		for (EBListener l : ebListeners.values()) {
			l.stop();
		}
		for (JmsListener l : jmsListeners.values()) {
			l.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	public static Collection<EventBusToJmsEntry> load(File file)
			throws IOException {
		final String mappingJson = IOUtils.toString(new FileInputStream(file));
		return new JsonObjectCodec().decode(mappingJson,
				new TypeReference<List<EventBusToJmsEntry>>() {
				});
	}
}