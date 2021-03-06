package org.apache.rocketmq.client.biz.factory;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.CollectionUtils;

import org.apache.rocketmq.client.biz.config.Ini;
import org.apache.rocketmq.client.biz.event.RocketmqEvent;
import org.apache.rocketmq.client.biz.event.handler.AbstractRouteableMessageHandler;
import org.apache.rocketmq.client.biz.event.handler.EventHandler;
import org.apache.rocketmq.client.biz.event.handler.Nameable;
import org.apache.rocketmq.client.biz.event.handler.chain.HandlerChainManager;
import org.apache.rocketmq.client.biz.event.handler.chain.def.DefaultHandlerChainManager;
import org.apache.rocketmq.client.biz.event.handler.chain.def.PathMatchingHandlerChainResolver;
import org.apache.rocketmq.client.biz.event.handler.impl.RocketmqEventMessageHandler;

public class MQEventHandlerFactoryBean implements FactoryBean<EventHandler<RocketmqEvent>> {

	/**
	 * 处理器定义
	 */
	private Map<String, EventHandler<RocketmqEvent>> handlers;
	
	/**
	 * 处理器链定义
	 */
	private Map<String, String> handlerChainDefinitionMap;
	
	private AbstractRouteableMessageHandler<RocketmqEvent> instance;

	public MQEventHandlerFactoryBean() {
		handlers = new LinkedHashMap<String, EventHandler<RocketmqEvent>>();
		handlerChainDefinitionMap = new LinkedHashMap<String, String>();
	}

	@Override
	public EventHandler<RocketmqEvent> getObject() throws Exception {
		if(instance == null){
			instance = createInstance();
		}
		return instance;
	}

	@Override
	public Class<?> getObjectType() {
		return RocketmqEventMessageHandler.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
	
	public Map<String, String> getHandlerChainDefinitionMap() {
		return handlerChainDefinitionMap;
	}

	public void setHandlerChainDefinitionMap(Map<String, String> handlerChainDefinitionMap) {
		this.handlerChainDefinitionMap = handlerChainDefinitionMap;
	}

	public Map<String, EventHandler<RocketmqEvent>> getHandlers() {
		return handlers;
	}

	public void setHandlers(Map<String, EventHandler<RocketmqEvent>> handlers) {
		this.handlers = handlers;
	}
	
	public void setHandlerChainDefinitions(String definitions) {
        Ini ini = new Ini();
        ini.load(definitions);
        Ini.Section section = ini.getSection("urls");
        if (CollectionUtils.isEmpty(section)) {
            section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
        }
        setHandlerChainDefinitionMap(section);
    }
	
	protected HandlerChainManager<RocketmqEvent> createHandlerChainManager() {

		HandlerChainManager<RocketmqEvent> manager = new DefaultHandlerChainManager();
		Map<String, EventHandler<RocketmqEvent>> handlers = getHandlers();
		if (!CollectionUtils.isEmpty(handlers)) {
			for (Map.Entry<String, EventHandler<RocketmqEvent>> entry : handlers.entrySet()) {
				String name = entry.getKey();
				EventHandler<RocketmqEvent> handler = entry.getValue();
				if (handler instanceof Nameable) {
					((Nameable) handler).setName(name);
				}
				manager.addHandler(name, handler);
			}
		}

		Map<String, String> chains = getHandlerChainDefinitionMap();
		if (!CollectionUtils.isEmpty(chains)) {
			for (Map.Entry<String, String> entry : chains.entrySet()) {
				// topic/tags/keys
				String url = entry.getKey();
				String chainDefinition = entry.getValue();
				manager.createChain(url, chainDefinition);
			}
		}

		return manager;
	}
	
	protected AbstractRouteableMessageHandler<RocketmqEvent> createInstance() throws Exception {
		HandlerChainManager<RocketmqEvent> manager = createHandlerChainManager();
        PathMatchingHandlerChainResolver chainResolver = new PathMatchingHandlerChainResolver();
        chainResolver.setHandlerChainManager(manager);
        return new RocketmqEventMessageHandler(chainResolver);
    }
	
	
	
}
