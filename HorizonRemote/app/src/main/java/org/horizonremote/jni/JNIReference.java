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

package org.horizonremote.jni;

/**
 * JNIReference is a pointer to a native JNIReference which holds a reference to a native ref_ptr ( a native reference counted object ).
 * As soon as the JNIReference is no longer reachable an internal WeakJNIReference will take care of disposing this pointer.
 * The actual garbage collection is done on a seperate thread in WeakJNIReference.
 *
 * @author rogro82
 */
public abstract class JNIReference {

    static {
        System.loadLibrary("horizonremote");
    }

    public static final long NULL_POINTER = 0;

    private long mReferencePointer;
    private WeakJNIReference mWeakJNIReference;

    protected JNIReference() {
        mReferencePointer = NULL_POINTER;
    }

    protected JNIReference(long refpointer) throws JNIReferenceException {
        setReferencePointer(refpointer);
    }

    protected void setReferencePointer(long refpointer)
            throws JNIReferenceException {

        /* throw if refpointer is NULL */
        if (refpointer == NULL_POINTER)
            throw new JNIReferenceException(
                    JNIReferenceException.EXCEPTION_NULL_POINTER);

        /* throw if mRefPointer was already set */
        if (mReferencePointer != NULL_POINTER)
            throw new JNIReferenceException(
                    JNIReferenceException.EXCEPTION_REDECLARE_POINTER);

        mReferencePointer = refpointer;
        mWeakJNIReference = new WeakJNIReference(this);
    }

    protected long getReferencePointer() {
        return mReferencePointer;
    }

    /**
     * Only call this if the object inherited checks the value of the
     * reference pointer before calling any native methods.
     */
    protected void disposeReference() {
        if (mReferencePointer != NULL_POINTER) {
            mReferencePointer = NULL_POINTER;
            mWeakJNIReference.dispose();
        }
    }

    protected boolean isValidReference() {
        return (mReferencePointer != NULL_POINTER);
    }

    /**
     * Never use the same reference pointer of one JNIReference
     * in another JNIReference but instead clone the actual reference
     * so that the native reference count gets raised else the backing
     * object will get disposed as soon as the original JNIReference
     * becomes available for garbage collection.
     * <p>
     * Cloning a JNIReferencePointer is in no way type-safe and should
     * only be done in special occasions like Parcelable objects.
     */

    protected static long cloneReferencePointer(JNIReference source) {
        return cloneReferencePointer(source.getReferencePointer());
    }

    protected static long cloneReferencePointer(long sourceRefPtr) {
        if (sourceRefPtr != NULL_POINTER) {
            return nativeCloneReference(sourceRefPtr);
        }
        return NULL_POINTER;
    }

    /*
     * native methods
     */
    protected static native void nativeDispose(long ptr);

    protected static native long nativeCloneReference(long ptr);

    /**
     * JNIReferenceException thown when trying to set JNIReference to NULL or
     * when calling setRefPointer on a JNIReference which has already been set.
     */

    public static class JNIReferenceException extends Exception {

        private static final long serialVersionUID = -5165119629386669573L;

        public static final int EXCEPTION_NULL_POINTER = 0;
        public static final int EXCEPTION_REDECLARE_POINTER = 1;

        public final int what;

        public JNIReferenceException(int what) {
            super(getExceptionMessage(what));

            this.what = what;
        }

        private static String getExceptionMessage(int what) {
            String msg;

            switch (what) {
                case EXCEPTION_NULL_POINTER:
                    msg = "Trying to use NULL pointer";
                    break;
                case EXCEPTION_REDECLARE_POINTER:
                    msg = "Trying to redeclare pointer";
                    break;
                default:
                    msg = "Unknown exception";
                    break;
            }

            return msg;
        }
    }
}