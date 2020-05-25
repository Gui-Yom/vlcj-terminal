### vlcj-terminal
Play videos in your terminal (very cool)

Usage :
```shell script
git clone https://github.com/Gui-Yom/vlcj-terminal.git
cd vlcj-terminal
./gradlew shadowJar
java -jar build/libs/vlcj-terminal.jar <Mode> <Width> <Height> <MRL>
5. Profit !
```
Mode is one of **ANSI** or **RGB**.

[MRL means media resource locator](https://wiki.videolan.org/Media_resource_locator/).
vlcj-terminal actually uses libvlc (its java port : vlcj) under the hood.
Wich means you can read nearly everything with it, from a simple mp4 file to a live remote video stream.
