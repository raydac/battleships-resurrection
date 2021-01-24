/*
Copyright (c) 2001 Gamefederation AB.
All rights reserved.
*/

package com.gamefederation.playmaker.client.j2me;

/**
 * One class in the (java part of the) component must implement this interface.
 */
public interface IComponentStateControl
{
	/**
	 * Starts the component
	 */
	public void start();

	/**
	 * Pauses the component
	 */
	public void pause();
	
	/**
	 * Stops the component
	 */
	public void stop();

	/**
	 * Initializes the component
	 *
	 * @param componentId A unique identifier of the component instance.
	 * @param clientId A unique identifier of the client running this component.
	 * @param server A host:port combination pointing at the server.
	 */
	public void init(int componentId, int clientId, String server);
}
