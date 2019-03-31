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

#ifndef REMOTE_HPP_
#define REMOTE_HPP_

#include "boost/ref_ptr.h"
#include "vnc/vnc_client.hpp"
#include "keys.hpp"

#define HORIZON_PORT	"5900"

namespace horizonremote {

class RemoteController :
	public Referenceable {
public:
	enum State {
		STATE_FAILURE		= -2,
		STATE_DISCONNECTED 	= -1,
		STATE_CONNECTING 	= 0,
		STATE_CONNECTED 	= 1,
	};

	RemoteController(const std::string& addr);

	bool	connect();
	void 	disconnect();

	State  	state();
	void 	send_key(unsigned short keycode, bool keydown=true);
	void 	toggle_key(unsigned short keycode);
	bool	poll();

protected:
	virtual ~RemoteController();

private:
	Network::VncClient client;
};

} /* namespace horizon */
#endif /* REMOTE_HPP_ */
