/*
 * Copyright 2013 Rob Groenendijk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <iostream>
#include "remote.hpp"
#include "keys.hpp"

namespace horizonremote {

RemoteController::RemoteController(const std::string& addr) :
	client(addr.c_str(), HORIZON_PORT) {
}

RemoteController::~RemoteController() {
}

bool RemoteController::connect() {
	bool connected;

	while (client.update()) {
	  if ((connected=client.connected())
			  || client.error_code() > 0)
	    break;
	}

	return connected;
}

void RemoteController::disconnect() {
	client.disconnect();
}

RemoteController::State  RemoteController::state() {
	return client.connected()
			? RemoteController::STATE_CONNECTED
			: client.error_code() > 0
			  ? RemoteController::STATE_FAILURE
			  : RemoteController::STATE_DISCONNECTED;
}

void RemoteController::send_key(unsigned short keycode, bool down) {
	client.send_key(keycode, down);
	client.update();
}

void RemoteController::toggle_key(unsigned short keycode) {
	client.pulse_key(keycode);
	client.update();
}

bool RemoteController::poll() {
	return client.update();
}

} /* namespace horizon */

#ifdef ANDROID

//======= JNI binding

#include "jni/jnireference.hpp"
#include "jni/org_horizonremote_RemoteController.h"

JNIEXPORT jlong JNICALL Java_org_horizonremote_RemoteController_nativeCreate
  (JNIEnv *env, jclass clazz, jstring jaddr) {

	const char *addr = env->GetStringUTFChars(jaddr, 0);

	ref_ptr<horizonremote::RemoteController> controller = new horizonremote::RemoteController(addr);

	env->ReleaseStringUTFChars(jaddr, addr);

	JNIReference* ref = new JNIReference(controller.get());
	return (jlong) ref;
}

/*
 * Class:     org_horizonremote_RemoteController
 * Method:    nativeConnect
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_horizonremote_RemoteController_nativeConnect
  (JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<horizonremote::RemoteController> controller = JNIReference::cast<horizonremote::RemoteController*>(jptr);
	controller->connect();
}

/*
 * Class:     org_horizonremote_RemoteController
 * Method:    nativeDisconnect
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_horizonremote_RemoteController_nativeDisconnect
  (JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<horizonremote::RemoteController> controller = JNIReference::cast<horizonremote::RemoteController*>(jptr);
	controller->disconnect();
}

/*
 * Class:     org_horizonremote_RemoteController
 * Method:    nativeState
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_horizonremote_RemoteController_nativeState
  (JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<horizonremote::RemoteController> controller = JNIReference::cast<horizonremote::RemoteController*>(jptr);
	return (jint)controller->state();
}

/*
 * Class:     org_horizonremote_RemoteController
 * Method:    nativeSendKey
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_org_horizonremote_RemoteController_nativeSendKey
  (JNIEnv *env, jclass clazz, jlong jptr, jint keycode, jboolean keydown) {

	ref_ptr<horizonremote::RemoteController> controller = JNIReference::cast<horizonremote::RemoteController*>(jptr);
	controller->send_key(keycode, keydown);
}

/*
 * Class:     org_horizonremote_RemoteController
 * Method:    nativeToggleKey
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_horizonremote_RemoteController_nativeToggleKey
  (JNIEnv *env, jclass clazz, jlong jptr, jint keycode) {

	ref_ptr<horizonremote::RemoteController> controller = JNIReference::cast<horizonremote::RemoteController*>(jptr);
	controller->toggle_key(keycode);
}

/*
 * Class:     org_horizonremote_RemoteController
 * Method:    nativePoll
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_horizonremote_RemoteController_nativePoll
(JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<horizonremote::RemoteController> controller = JNIReference::cast<horizonremote::RemoteController*>(jptr);
	return (jint)controller->poll();
}

#endif
