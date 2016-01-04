# ChordBittorrent
Simple Peer-to-Peer file sharing demo application.

## How to Run

- First run compile.bat or compile.sh depending on your platform. It will compile all the java files into
class files and keep the package structure of the project together.

- From the same directory, you can now start the server, which will split the file and wait for
incoming peer connections to distribute file chunks (on port 8888)

    - Run: `java com.bittorrent.server.Server <file to split>`

- You may now start 5 peers in the following way

    - Run: `java com.bittorrent.client.Peer <file owner port> <listening port> <neighbor listening port>`

- Each client will create their own personal directory to write files. 
    
    - For each Peer, the directory created will be: `client/<listening port>`
