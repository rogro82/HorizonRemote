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

#include <jni/jnireference.hpp>

namespace horizonremote {

JNIReference::JNIReference(Referenceable *object) {
	this->m_refobj = object;
}

JNIReference::~JNIReference() {

	this->m_refobj = NULL;
}

Referenceable* JNIReference::get() {
	return this->m_refobj.get();
}

void JNIReference::dispose(jpointer ptr) {

	JNIReference* ref = static_cast<JNIReference*>((void*) ptr);
	if (ref)
		delete (ref);
}

}

JNIEXPORT void JNICALL Java_org_horizonremote_jni_JNIReference_nativeDispose(
		JNIEnv *env, jclass clazz, jpointer ptr) {

	JNIReference::dispose(ptr);
}

JNIEXPORT jlong JNICALL Java_org_horizonremote_jni_JNIReference_nativeCloneReference(
		JNIEnv *env, jclass clazz, jlong src) {

	/* create a new JNIReference which points to the same Referenceable as src */

	JNIReference* clonedRef = new JNIReference(
			JNIReference::cast<Referenceable*>(src));

	return (jlong) clonedRef;
}
