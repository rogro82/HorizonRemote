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

#ifndef JNIREFERENCE_H_
#define JNIREFERENCE_H_

#include <boost/ref_ptr.h>
#include <jni.h>
#include "org_horizonremote_jni_JNIReference.h"

typedef jlong jpointer;

namespace horizonremote {

class JNIReference {
public:

	JNIReference(Referenceable* object);

	Referenceable* get();

	/*
	 * fast implementation to unwrap from a JNI jlong to a given derived class of Referenceable
	 * */
	template<class T>
	static inline T cast(jpointer ptr) {
		return static_cast<T>(static_cast<JNIReference*>((void*) ptr)->get());
	}

	static void dispose(jpointer ptr);

private:

	virtual ~JNIReference();
	ref_ptr<Referenceable> m_refobj;
};

}

typedef horizonremote::JNIReference JNIReference;

#endif /* JNIREFERENCE_H_ */
