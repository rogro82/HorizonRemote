#include "vnc/vnc_client.hpp"
#include "vnc/des_local.h"
         
#include <iostream>
#include <cstring>
#include <stdlib.h>

namespace Network
{	
  VncClient::VncClient(const char* hostname, const char* port)
    : RawStream(hostname, port),
      _proto_lo_version(0),
      _proto_hi_version(0),
      _security_type(0),
      _state(vnc_waiting_for_version),
      _width(0),
      _height(0),
      _bpp(0),
      _keep_framebuffer(false),
      _framebuffer_version(0)
  {
  }

  VncClient::~VncClient()
  {
  }

  bool VncClient::connected() const
  {
    return _state == vnc_connected;
  }

  const char* VncClient::username() const
  {
    return _username.c_str();
  }

  const char* VncClient::password() const
  {
    return _password.c_str();
  }

  void VncClient::set_password(const char* password)
  {
    _password = password;

    if (error_code() == STREAM_VNC_PASSWORD_REQUIRED)
      reset_error();
  }

  void VncClient::set_password(const char* username, const char* password)
  {
    _username = username;
    _password = password;

    if (error_code() == STREAM_VNC_PASSWORD_REQUIRED)
      reset_error();
    if (error_code() == STREAM_VNC_USERNAME_PASSWORD_REQUIRED)
      reset_error();
  }

  void VncClient::set_keep_framebuffer(bool keep)
  {
    _keep_framebuffer = keep;
  }

  int VncClient::framebuffer_width() const
  {
    return _width;
  }

  int VncClient::framebuffer_height() const
  {
    return _height;
  }

  int VncClient::framebuffer_bpp() const
  {
    return _bpp;
  }

  int VncClient::framebuffer_version() const
  {
    return _framebuffer_version;
  }

  const char* VncClient::framebuffer() const
  {
    return _framebuffer.size() > 0 ? &_framebuffer[0] : NULL;
  }

  bool VncClient::update(float timeout)
  {
    if (!RawStream::update(timeout))
      return false;

    if (state() == state_connected)
    {
      switch (_state)
      {
        case vnc_waiting_for_version:
          rfb_wait_for_version();
          break;
        case vnc_waiting_for_security_server:
          rfb_wait_for_security_server();
          break;
        case vnc_waiting_for_security_handshake:
          rfb_wait_for_security_handshake();
          break;
        case vnc_authenticate:
          rfb_authenticate();
          break;
        case vnc_waiting_for_vnc_challenge:
          rfb_wait_for_vnc_challenge();
          break;
        case vnc_waiting_for_ard_challenge:
          rfb_wait_for_ard_challenge();
          break;
        case vnc_waiting_for_security_result:
          rfb_wait_for_security_result();
          break;
        case vnc_waiting_for_protocol_failure_reason:
          rfb_wait_for_protocol_failure_reason();
          break;
        case vnc_initialize:
          rfb_initialize();
          break;
        case vnc_waiting_for_server_initialization:
          rfb_wait_for_server_initialization();
          break;
        case vnc_setup:
          rfb_setup();
          break;
        case vnc_connected:
          rfb_connected();
          break;
        case vnc_protocol_failure:
          break;
      }
    }

    return true;
  }  

  void VncClient::rfb_wait_for_version()
  {
    std::string& r = response();

    if (r.size() >= 12)
    {
      if (r[0] != 'R' || r[1] != 'F' || r[2] != 'B' || r[3] != ' ')
      {
        set_error(STREAM_VNC_PROTOCOL_ERROR, "Unknown remote control protocol.");

        _state = vnc_protocol_failure;        
      }
      else
      {
        // Simply respond with the same protocol version as we've got from server.
        write(&*r.begin(), &*r.begin() + 12);

        char version_hi[] = { r[4] != '0' ? r[4] : (r[5] != '0' ? r[5] : r[6]), 0 };
        char version_lo[] = { r[8] != '0' ? r[8] : (r[9] != '0' ? r[9] : r[10]), 0 };

        _proto_hi_version = atoi(version_hi);
        _proto_lo_version = atoi(version_lo);

        eat(12);        

        if (_proto_hi_version == 3 && _proto_lo_version < 7)
          _state = vnc_waiting_for_security_server;
        else
          _state = vnc_waiting_for_security_handshake;
      }
    }
  }

  void VncClient::rfb_wait_for_security_server()
  {
    std::string& r = response();

    if (r.length() >= 4)
    {
      int security_protocol = *(int *)&*r.begin();
      if (security_protocol == 0)
      {
        set_error(STREAM_VNC_PROTOCOL_ERROR, "Server refused remote control connection.");

        _state = vnc_protocol_failure;        
      }
      else
      {
        _security_type = security_protocol;
        
        _state = vnc_authenticate;

        eat(4);
      }
    }
  }

  void VncClient::rfb_wait_for_security_handshake()
  {
    std::string& r = response();

    if (r.length() > 0)
    {
      int protocol_count = (int)r[0];

      if (protocol_count == 0)
      {        
        set_error(STREAM_VNC_PROTOCOL_ERROR, "Server refused remote control connection.");

        _state = vnc_waiting_for_protocol_failure_reason;

        eat(1);
      }

      if (protocol_count > 0 && (int)r.length() >= 1 + protocol_count)
      {
        int choosen_protocol = -1;

        // Check for preferred authentication type.
        for (int i = 0; i < protocol_count; ++i)
        {
          if (r[i + 1] == 30 /* ARD, Mac authentication */)
          {
            choosen_protocol = i + 1;
            break;
          }
        }

        // Check for supported authentication types.
        if (choosen_protocol < 0) 
        {
          for (int i = 0; i < protocol_count; ++i)
          {
            if (r[i + 1] == 1 /* No authentication */ || r[i + 1] == 2 /* VNC authentication */ || r[i + 1] == 16 /* Tight authentication */)
            {
              choosen_protocol = i + 1;
              break;
            }
          }
        }

        if (choosen_protocol >= 0)
        {
          write(&*r.begin() + choosen_protocol, &*r.begin() + choosen_protocol + 1);

          _security_type = r[choosen_protocol];

          _state = vnc_authenticate;

          eat(protocol_count + 1);
        }
        else
        {
          set_error(STREAM_VNC_PROTOCOL_ERROR, "Server does not support requested authentication mode.");

          eat(protocol_count + 1);
        }
      }
    }
  }

  void VncClient::rfb_authenticate()
  {
    if (_security_type == 1 /* No authentication */)
    {
      rfb_authenticate_none();
    }
    if (_security_type == 2 /* VNC authentication */)
    {
      rfb_authenticate_vnc();
    }
    if (_security_type == 16 /* Tight authentication */) 
    {
      rfb_authenticate_tight();
    }
    if (_security_type == 30 /* ARD authentication */) 
    {
      rfb_authenticate_ard();
    }
  }

  void VncClient::rfb_authenticate_none()
  {
    if (_proto_hi_version == 3 && _proto_lo_version <= 7)
    {
      _state = vnc_initialize;
    }
    else
    {
      _state = vnc_waiting_for_security_result;
    }
  } 

  void VncClient::rfb_authenticate_vnc()
  {
    _state = vnc_waiting_for_vnc_challenge;
  }

  void VncClient::rfb_authenticate_ard()
  {
    _state = vnc_waiting_for_ard_challenge;
  }

  void VncClient::rfb_wait_for_ard_challenge()
  {
	  /* REMOVED IMPLEMENTATION */
  }

  void VncClient::rfb_wait_for_vnc_challenge()
  {
    std::string& r = response();

    if (r.length() >= 16)
    {
      if (_password.length() == 0)
      {
        set_error(STREAM_VNC_PASSWORD_REQUIRED, "Your password is needed.");
      }
      else
      {
        // Encrypt challenge with password.

        // Reverse bit order in the key.
        unsigned char key[8];

        for (size_t i = 0; i < 8; ++i)
        {
          if (i < _password.length())
            key[i] = (unsigned char)_password[i];
          else
            key[i] = 0;
        }        

        // Encrypt.
        deskey(key, EN0);

        unsigned char challenge[16];
        for (int i = 0; i < 16; ++i)
          challenge[i] = (unsigned char)r[i];

        for (int i = 0; i < 16; i += 8)
          des(challenge + i, challenge + i);

        // Send it back.
        write((char *)challenge, (char *)challenge + 16);

        eat(16);

        _state = vnc_waiting_for_security_result;
      }
    }
  }

  void VncClient::rfb_authenticate_tight()
  {
  }

  void VncClient::rfb_wait_for_security_result()
  {
    std::string& r = response();

    if (r.length() >= 4)
    {
      unsigned int security_result = byte_swap(*(unsigned int *)&*r.begin());
      if (security_result == 0)
      { 
        _state = vnc_initialize;

        eat(4);
      }
      else
      {
        if (security_result == 1)
          set_error(STREAM_VNC_LOGIN_FAILED, "Unable to login to server.");
        
        if (security_result == 2)
          set_error(STREAM_VNC_LOGIN_FAILED, "Too many attempts to login to server.");

        _state = vnc_waiting_for_protocol_failure_reason;

        eat(4);
      }
    }
  }

  void VncClient::rfb_wait_for_protocol_failure_reason()
  {
    //TODO: For now, just go into failure mode.

    _state = vnc_protocol_failure;

    if (_proto_hi_version == 3 && _proto_lo_version <= 7)
    {
      _state = vnc_protocol_failure;
    }
    else
    {
      std::string& r = response();

      if (r.length() >= 4)
      {
        unsigned int length = byte_swap(*(unsigned int *)&*r.begin());

        if (r.length() >= 4 + length)
        {
          _message.assign(&r[4], &r[4] + length);

          _state = vnc_protocol_failure;

          eat(4 + length);
        }
      }    
    }
  }

  void VncClient::rfb_initialize()
  {
    // Ask for shared session.
    char shared[] = { 1 };

    write(shared, shared + 1);

    _state = vnc_waiting_for_server_initialization;
  }

  void VncClient::rfb_wait_for_server_initialization()
  {
    std::string& r = response();

    if (r.length() >= 24)
    {
      unsigned int name_length = byte_swap(*(unsigned int *)(&*r.begin() + 20));
      if (r.length() >= 24 + name_length)
      {
        _width = (int)(byte_swap(*(unsigned short *)(&*r.begin() + 0)));
        _height = (int)(byte_swap(*(unsigned short *)(&*r.begin() + 2)));

        _bpp = (int)(*(unsigned char *)(&*r.begin() + 4)) / 8;

        _name.assign(r.begin() + 24, r.begin() + 24 + name_length);

        _state = vnc_setup;

        eat(24 + name_length);
      }
    }
  }

  void VncClient::rfb_setup()
  {
    // Set up raw encoding type. We don't ask for screens anyway.
    char encoding[] = { 2, 0, 0, 1, 0, 0, 0, 0 };

    write(encoding, encoding + sizeof(encoding) / sizeof(char));

    _state = vnc_connected;
  }

  void VncClient::rfb_connected()
  {
    std::string& r = response();

    if (r.length() >= 1)
    {
      switch (r[0])
      {
        case 0: /* Framebuffer update */
          rfb_framebuffer_update();
          break;
        case 1: /* Colormap entries */
          rfb_set_color_map();
          break;
        case 2: /* Bell */
          rfb_bell();
          break;
        case 3: /* Clipboard */
          rfb_set_clipboard();
          break;
        default:
          set_error(STREAM_VNC_UNSUPPORTED, "Server sent unsupported message.");
          _state = vnc_protocol_failure;
          break;
      }
    }    
  }

  void VncClient::rfb_framebuffer_update()
  {
    std::string& r = response();

    if (r.length() >= 4)
    {
      int length = (int)byte_swap(*(unsigned short *)(&*r.begin() + 2));

      bool complete = true;

      int current = 4;

      for (; length > 0; --length)
      {
        if (r.length() >= current + 12)
        { 
          int x = byte_swap(*(unsigned short *)(&*r.begin() + current + 0));
          int y = byte_swap(*(unsigned short *)(&*r.begin() + current + 2));
          int width = byte_swap(*(unsigned short *)(&*r.begin() + current + 4));
          int height = byte_swap(*(unsigned short *)(&*r.begin() + current + 6));
          int type = byte_swap(*(unsigned int *)(&*r.begin() + current + 8));

          if (type != 0)
          {
            set_error(STREAM_VNC_UNSUPPORTED, "Server sent unsupported message.");

            _state = vnc_protocol_failure;

            complete = false;
            
            break;
          }
          else
          {
            int pixel_length = width * height * _bpp;
            if (r.length() >= current + 12 + pixel_length)
            {
              current += 12 + pixel_length;

              // Copy pixel data.
              if (_keep_framebuffer) 
              {
                if (_framebuffer.size() < _width * _height * _bpp)
                  _framebuffer.resize(_width * _height * _bpp);

                int left = std::min(x, _width);
                int bytes_per_line = (std::min(x + width, _width) - left) * _bpp;;

                for (int top = y, last_top = std::min(_height, y + height); top < last_top; ++top)
                  std::memcpy(&_framebuffer[0] + (top * _width + left) * _bpp, r.data() + (top - y) * width * _bpp, bytes_per_line);                

                ++_framebuffer_version;
              }              
            }
            else
            {
              complete = false;

              break;
            }
          }
        }
        else
        {
          complete = false;
          break;
        }
      }

      if (complete)
        eat(current);
    }
  }

  void VncClient::rfb_set_color_map()
  {
    std::string& r = response();

    if (r.length() >= 6)
    {
      unsigned short length = byte_swap(*(unsigned short *)(&*r.begin() + 4));

      if (r.length() >= 6 + length * 6)
      {
        eat(6 + length * 6);
      }
    }
  }
   
  void VncClient::rfb_bell()
  {    
    std::string& r = response();

    if (r.length() >= 8)
    {
      eat(1);
    }
  }
   
  void VncClient::rfb_set_clipboard()
  {
    std::string& r = response();

    if (r.length() >= 8)
    {
      unsigned int length = byte_swap(*(unsigned int *)(&*r.begin() + 4));
      if (r.length() >= 8 + length)
      {
        eat(8 + length);
      }
    }
  }

  void VncClient::pulse_key(unsigned short key)
  {
    send_key(key, true);
    send_key(key, false);
  }
  
  void VncClient::send_key(unsigned short key, bool down)
  {    
    char key_event[] = { 4, (char)(down ? 1 : 0), 0, 0, 0, 0, (char)((key & 0xff00) >> 8), (char)(key & 0x00ff) };

    write(key_event, key_event + sizeof(key_event) / sizeof(char));
  }

  void VncClient::request_screen(bool incremental, int x, int y, int width, int height)
  {
    char frame_event[] = {
      3,
      (char)(incremental ? 1 : 0),
      (char)((x & 0xff00) >> 8),
      (char)(x & 0xff),
      (char)((y & 0xff00) >> 8),
      (char)(y & 0xff),
      (char)((width & 0xff00) >> 8),
      (char)(width & 0xff),
      (char)((height & 0xff00) >> 8),
      (char)(height & 0xff)
    };

    write(frame_event, frame_event + sizeof(frame_event) / sizeof(char));
  }

  unsigned int VncClient::byte_swap(unsigned int v)
  {
    return 
      ((v & 0x000000ff) << 24) |
      ((v & 0x0000ff00) << 8) | 
      ((v & 0x00ff0000) >> 8) |
      ((v & 0xff000000) >> 24);
  }

  unsigned short VncClient::byte_swap(unsigned short v)
  {
    return 
      ((v & 0x00ff) << 8) |
      ((v & 0xff00) >> 8);
  }
}
