package org.horizonremote;

import org.horizonremote.IControllerCallback;

interface IControllerService {
	void registerCallback(in IControllerCallback cb, int id);
	void unregisterCallback(in IControllerCallback cb);
	void dispatchEvent(int id, int what, int data);
}