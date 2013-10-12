#ifndef SMART_PTR_INTRUSIVE_PTR_H
#define SMART_PTR_INTRUSIVE_PTR_H

//
// ref_ptr.h
//
// Copyright (c) 2001, 2002 Peter Dimov
//
// Distributed under the Boost Software License, Version 1.0. (See
// accompanying file LICENSE_1_0.txt or copy at
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/smart_ptr/ref_ptr.html for documentation.
//
// Modified by elc@telecom-paristech, Oct.2011:
// - ref_ptr can be used without installing the Boost distribution
// - added definition of Pointable, a generic base class with a compatible counter
// - misc. checkings if macro SMART_PTR_DEBUG is defined before including this file
//   (and debug messages on stderr if SMART_PTR_DEBUG_MESSAGES is defined)
//
// Modified by Rob Groenendijk, Jan.2013:
// - Android specific modifications and some unneeded debug clean-up

#include <ostream>
#define BOOST_SP_NO_SP_CONVERTIBLE

/** Generic base class for objects that can be pointed by an ref_ptr.
 * - @see ref_ptr for details
 * - misc. checkings are performed if macro SMART_PTR_DEBUG is defined before
 *   including ref_ptr.h
 * - debug messages are displayed on stderr if macros SMART_PTR_DEBUG and
 *   if SMART_PTR_DEBUG_MESSAGES are both defined.
 */

class Referenceable {
public:
	Referenceable() :
			_ref_count(0) {
	}

	// NB: the destructor MUST be virtual.
	virtual ~Referenceable() {
	}

	// NB: the counter must not be copied.
	Referenceable(const Referenceable&) :
			_ref_count(0) {
	}
	Referenceable& operator=(const Referenceable&) {
		return *this;
	}

	int getReferenceCount() {
		return _ref_count;
	}

private:
	// number of smart pointers currently pointing to this object.
	long _ref_count;

	friend long ref_ptr_get_count(Referenceable* p);
	// Boost functions required by ref_ptr.
	friend void ref_ptr_add_ref(Referenceable* p);
	friend void ref_ptr_release(Referenceable* p);
};

inline long ref_ptr_get_count(Referenceable* p) {

	return p->_ref_count;
}

inline void ref_ptr_add_ref(Referenceable* p) {

	__sync_add_and_fetch(&p->_ref_count, 1);
}

inline void ref_ptr_release(Referenceable* p) {

	if(__sync_sub_and_fetch(&p->_ref_count, 1) == 0) delete p;
}

/** ref_ptr: a smart pointer that uses intrusive reference counting.
 *  Relies on unqualified calls to
 * <pre>
 *      void ref_ptr_add_ref(T * p);
 *      void ref_ptr_release(T * p);
 * </pre>
 *           (p != 0)
 *
 *  The object is responsible for destroying itself.
 */
template<class T> class ref_ptr {
private:

	typedef ref_ptr this_type;
	typedef T * this_type::*unspecified_bool_type;

public:

	typedef T element_type;

	ref_ptr() :
			px(0) {
	}

	ref_ptr(T * p, bool add_ref = true) :
			px(p) {
		if (px != 0 && add_ref)
			ref_ptr_add_ref(px);
	}

#if !defined(BOOST_NO_MEMBER_TEMPLATES) || defined(BOOST_MSVC6_MEMBER_TEMPLATES)

	template<class U>
#if !defined( BOOST_SP_NO_SP_CONVERTIBLE )
	ref_ptr( ref_ptr<U> const & rhs, typename detail::sp_enable_if_convertible<U,T>::type = detail::sp_empty() )
#else
	ref_ptr(ref_ptr<U> const & rhs)
#endif
	:
			px(rhs.get()) {
		if (px != 0)
			ref_ptr_add_ref(px);
	}

#endif

	ref_ptr(ref_ptr const & rhs) :
			px(rhs.px) {
		if (px != 0)
			ref_ptr_add_ref(px);
	}

	~ref_ptr() {
		if (px != 0)
			ref_ptr_release(px);
	}

#if !defined(BOOST_NO_MEMBER_TEMPLATES) || defined(BOOST_MSVC6_MEMBER_TEMPLATES)

	template<class U> ref_ptr & operator=(ref_ptr<U> const & rhs) {
		this_type(rhs).swap(*this);
		return *this;
	}

#endif

	// Move support
#if defined( BOOST_HAS_RVALUE_REFS )

	ref_ptr(ref_ptr && rhs): px( rhs.px )
	{
		rhs.px = 0;
	}

	ref_ptr & operator=(ref_ptr && rhs)
	{
		this_type(std::move(rhs)).swap(*this);
		return *this;
	}

#endif

	ref_ptr & operator=(ref_ptr const & rhs) {
		this_type(rhs).swap(*this);
		return *this;
	}

	ref_ptr & operator=(T * rhs) {
		this_type(rhs).swap(*this);
		return *this;
	}

	void reset() {
		this_type().swap(*this);
	}

	void reset(T * rhs) {
		this_type(rhs).swap(*this);
	}

	T * get() const {
		return px;
	}

	T & operator*() const {
		return *px;
	}

	T * operator->() const {
		return px;
	}

	// implicit conversion to "bool"
	//#include <boost/smart_ptr/detail/operator_bool.hpp>
	operator unspecified_bool_type() const // never throws
	{
		return px == 0 ? 0 : &this_type::px;
	}

	bool operator!() const // never throws
	{
		return px == 0;
	}

	void swap(ref_ptr & rhs) {
		T * tmp = px;
		px = rhs.px;
		rhs.px = tmp;
	}

private:

	T * px;
};

template<class T, class U> inline bool operator==(ref_ptr<T> const & a,
		ref_ptr<U> const & b) {
	return a.get() == b.get();
}

template<class T, class U> inline bool operator!=(ref_ptr<T> const & a,
		ref_ptr<U> const & b) {
	return a.get() != b.get();
}

template<class T, class U> inline bool operator==(ref_ptr<T> const & a, U * b) {
	return a.get() == b;
}

template<class T, class U> inline bool operator!=(ref_ptr<T> const & a, U * b) {
	return a.get() != b;
}

template<class T, class U> inline bool operator==(T * a, ref_ptr<U> const & b) {
	return a == b.get();
}

template<class T, class U> inline bool operator!=(T * a, ref_ptr<U> const & b) {
	return a != b.get();
}

#if __GNUC__ == 2 && __GNUC_MINOR__ <= 96

// Resolve the ambiguity between our op!= and the one in rel_ops

template<class T> inline bool operator!=(ref_ptr<T> const & a, ref_ptr<T> const & b)
{
	return a.get() != b.get();
}

#endif

template<class T> inline bool operator<(ref_ptr<T> const & a,
		ref_ptr<T> const & b) {
	return std::less<T *>()(a.get(), b.get());
}

template<class T> void swap(ref_ptr<T> & lhs, ref_ptr<T> & rhs) {
	lhs.swap(rhs);
}

// mem_fn support

template<class T> T * get_pointer(ref_ptr<T> const & p) {
	return p.get();
}

template<class T, class U> ref_ptr<T> static_pointer_cast(
		ref_ptr<U> const & p) {
	return static_cast<T *>(p.get());
}

template<class T, class U> ref_ptr<T> const_pointer_cast(ref_ptr<U> const & p) {
	return const_cast<T *>(p.get());
}

template<class T, class U> ref_ptr<T> dynamic_pointer_cast(
		ref_ptr<U> const & p) {
	return dynamic_cast<T *>(p.get());
}

// operator<<

#if !defined(BOOST_NO_IOSTREAM)

#if defined(BOOST_NO_TEMPLATED_IOSTREAMS) || ( defined(__GNUC__) &&  (__GNUC__ < 3) )

template<class Y> std::ostream & operator<< (std::ostream & os, ref_ptr<Y> const & p)
{
	os << p.get();
	return os;
}

#else

// in STLport's no-iostreams mode no iostream symbols can be used
#ifndef _STLP_NO_IOSTREAMS
template<class E, class T, class Y> std::basic_ostream<E, T> & operator<<(
		std::basic_ostream<E, T> & os, ref_ptr<Y> const & p) {
	os << p.get();
	return os;
}

#endif // _STLP_NO_IOSTREAMS
#endif // __GNUC__ < 3
#endif // !defined(BOOST_NO_IOSTREAM)

#ifdef BOOST_MSVC
# pragma warning(pop)
#endif

#endif
