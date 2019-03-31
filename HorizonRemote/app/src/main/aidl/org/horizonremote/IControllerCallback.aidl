package org.horizonremote;

interface IControllerCallback {
	void OnControllerStateUpdate(int id, int state);
}