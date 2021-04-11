# Author: Daniil Grbic
# License: MIT

from websocket_server_Pithikos.websocket_server import WebsocketServer
import pyautogui
import socket

pyautogui.PAUSE = 0         # this makes cursor go buttery smooth
pyautogui.FAILSAFE = False  # this stops the user from accidentally killing the server
debug = False               # change this to true for a detailed server output

# ownIP is the LAN address of the server, the code is the last number of the adress
# NOTE: this only works for networks where the subnet mask is 255.255.255.0!
ownIP = socket.gethostbyname(socket.gethostname())

# We need a workaround for Linux machines (since gethostbyname() returns 127.x.x.x)
if ownIP.split('.')[0] == "127":
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ownIP = s.getsockname()[0]
    s.close()

print("READY!")
print("SERVER ACCESS CODE: %s" % ownIP.split('.')[-1])

if debug: 
    import time
    global serverReady
    serverReady = time.time()
    print("WARNING: In debug mode the server MAY FREEZE once interacted with using the mouse (e.g. when right-clicked)!")

def new_client(client, server):
    print("Connection established with client %s!" % client['address'][0])

def client_left(client, server):
    try:
        print("Client(%s) disconnected!" % client['address'][0])
        print("SERVER ACCESS CODE: %s" % ownIP.split('.')[-1])
    except:
        if debug:
            print("WARNING: Client disconnected but was never there?")

def message_received(client, server, message):
    if debug: 
        global serverReady
        Sec,MilSec = int(time.time()-serverReady), int(100*(time.time()-serverReady))%100
        print("{}:{} -> {}".format(Sec, MilSec, message))

    # moving the cursor as told by the client
    if message in ["CLICK","DOUBLE_CLICK"]:
        pyautogui.click()
    elif message == "START_DRAG":
        pyautogui.mouseDown()
    elif message[:6] == "SCROLL":
        pyautogui.scroll(int(message.split('.')[1]))
    elif message[:4] in ["DRAG","MOVI"]:
        pyautogui.move(int(message.split('.')[1]),int(message.split('.')[2]))
    elif message == "DROP":
        pyautogui.mouseUp()
    elif message == "RIGHT":
        pyautogui.rightClick()

# Server setup
# PORT 8080 should work for all machines and networks since it is used to access the Net
# localhost = 0.0.0.0
PORT=8080
HOST="0.0.0.0"

server = WebsocketServer(PORT, HOST)
server.set_fn_new_client(new_client)
server.set_fn_client_left(client_left)
server.set_fn_message_received(message_received)
server.run_forever()
