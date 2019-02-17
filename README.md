 

**HDR2EXR**
A plugin for the Ricoh Theta V to make one HDR exr file. 
Main use would be for on set VFX HDR capture.

***There are enough HDR apps. What makes this one different?***

1) It measures the on set lighting and bases it bracketing on that lighting situation. It takes 1 auto exposed picture to determine basic exposure settings. Based on that it set the lowest iso and then starts taking 11 bracked pictures with a 2 stop increase. This should be enough to capture almost every lighting situation. (I will experiment a bit more with this.) 

2) It automatically merges these 11 pictures into one EXR file ready to be used in NUKE, MAYA etc. (this is done through OpenCV hdr libraries.)

***How to install?***

See the ricoh theta V forum for help with installing plugins. 
https://plugin-dev-quickstart.readthedocs.io/en/latest/index.html
- Make sure to set the permissions for camera and disk through the use of vysor (or scrcpy).
- And make sure to set the plugin as the default plugin to use.

***How to use?***

1) Start the plugin by holding down the mode button for 2 seconds. The little led will turn white. And the wifi logo will blink in Magenta.

2) Put the camera on chosen location (use a tripod, shooting handheld will lead to crappy pictures) and push photo button. You have 3 seconds to run away and hide, else you are in the picture. (Will probably increase this to 5 sec.) 

3) The Wifi logo turns greens and the theta makes picture taking sounds. It takes 12 pictures (1 to measure lighting 11 brackets).

4) After the picture taking the wifi logo will turn blue. You can now move or pick up the camera. It is busy merging the pictures. This takes about a minute. When it is done it makes a sound and the wifi logo turn magenta again.

5) Connect camera to a computer to download the pictures.

 

***Credits***

- The picture taking part is largely based on the work of Ichi Hirota’s dual-fisheye plug-in <https://github.com/theta360developers/original-dual-fisheye-plugin>
- The integration of OpenCV is a combination of <https://community.theta360.guide/t/ricoh-blog-post-running-opencv-in-your-ricoh-theta/4084> and <https://www.learn2crack.com/2016/03/setup-opencv-sdk-android-studio.html> and a lot of trail and error!

Feel free to change, improve and of course use!

Let me know what you think and run into!



TODO v1

- - make colors en sounds better
- - exr half float support
- - clean up code?
- - do all dirs exist? if not create!
- - are all permissions okey? no error...



TODO v2

- - add web interface
- - turn sound on/off
- - turn iso looping on/off
- - exr half/full float on/off
- - download exr
- - name session
- - viewer
- - show status
- - stops step setting?
- - number of pics?

  

 

 

 

 
