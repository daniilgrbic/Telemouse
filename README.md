# Telemouse (Wifi Touchpad App)

Telemouse App can turn *any* Android smartphone into a fully functional computer trackpad. 

Once the Telemouse Server has been installed on a computer, a connection can be established via LAN (both devices need to be connected to the same network), and your phone sends instructions to the computer.

---

## Telemouse Server

Make sure your phone and computer are connected to the same network! Wifi, ethernet, and even hotspot will do (one of the devices can be a hotspot as well). You must let Telemouse server through firewall, otherwise it might not work.

#### Windows Setup

Install the windows installer from [releases page](https://github.com/daniilgrbic/Telemouse/releases/) and start it. After a short pause, you will see a message similar to the one in the pictures below. You will need to enter the 'server access code' in the Telemouse app when prompted. When you connect, a message should appear confirming a connection has been established successfully.

#### Linux & macOS Setup

Grab the code from [the repo](https://github.com/daniilgrbic/Telemouse/).
Install the latest version of [Python3](https://www.python.org/download/) and after that install [PyAutoGUI library](https://pyautogui.readthedocs.io/en/latest/install.html).

Start the server by running the **telemouse-server.py** file. You are done!

| ![](https://raw.githubusercontent.com/daniilgrbic/Telemouse/master/Screenshots/server-loaded.PNG) | ![](https://raw.githubusercontent.com/daniilgrbic/Telemouse/master/Screenshots/server-connection-established.PNG) |
| --- | --- |

---

## Telemouse App

When you run the Telemouse app, you will be prompted to enter the 'server access code'. Enter the code provided by the server (an integer in range 0-255) and press `Ok!` A dialogue should pop confirming that the connection was established successfully. 

| ![](https://raw.githubusercontent.com/daniilgrbic/Telemouse/master/Screenshots/app-ui.png) | ![](https://raw.githubusercontent.com/daniilgrbic/Telemouse/master/Screenshots/app-ui-elements.png) | ![](https://raw.githubusercontent.com/daniilgrbic/Telemouse/master/Screenshots/app-initial.png) | ![](https://raw.githubusercontent.com/daniilgrbic/Telemouse/master/Screenshots/app-connection-dialogue.png) |
| --- | --- | --- | --- |

---

### Compatibility

- Compatible with Android 6 and newer; tested on Android 6, 8, and 9
- Simple installer for Windows
- Runs on Linux and macOS, but there are no installers currently available (instead, you will need to install Python and run the code itself)