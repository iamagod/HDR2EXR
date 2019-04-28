 

**HDR2EXR**

A plugin for the Ricoh Theta V to make one HDR exr file. 
Main use would be for on set VFX HDR capture.

***There are enough HDR apps. What makes this one different?***

1) It measures the on set lighting and bases it bracketing on that lighting situation.
It takes 1 auto exposed picture to determine basic exposure settings.
Based on that it set the lowest iso and then starts taking pictures.
For each bracket it takes 3 pics and averages them together to reduce noise.
It takes 11 brackets with a 2.5 stop increase. This should be enough to capture almost every lighting situation.
Unfortunately very bright lights (like the sun) are still visible with lowest shutter times and iso.
This maybe can be fixed on the Z1 with higher aperture.

2) It automatically merges these 11 pictures into one EXR file ready to be used in NUKE, MAYA etc. (this is done through OpenCV hdr libraries.)

***How to install?***

See the ricoh theta V forum for help with installing plugins. 
https://plugin-dev-quickstart.readthedocs.io/en/latest/index.html
- Make sure to set the permissions for camera and disk through the use of vysor (or scrcpy).
- And make sure to set the plugin as the default plugin to use.

***How to use?***

1) Start the plugin by holding down the mode button for 2 seconds. The little led will turn white. And the wifi logo will turn Magenta.

2) Put the camera on chosen location (use a tripod, shooting handheld will lead to crappy pictures) and push photo button. You have 5 seconds to run away and hide, else you are in the picture.

3) The Wifi logo turns greens and the theta makes picture taking sounds. It takes 34 pictures (1 to measure lighting 3*11 brackets).

4) After the picture taking the wifi logo will blink red and blue. You can now move or pick up the camera. It is busy merging the pictures. This takes about one or two minutes. When it is done it makes a sound and the wifi logo turn magenta again.

5) Connect camera to a computer to download the pictures.


***Good to know***
It tries to keep the iso as low as possible but also the the exposure time, when exposure gets above 1 sec, it increases iso (until it runs out of iso and then increases exposure time again ;-)  .)
This version works with OpenCV 3.4.4 I ran into to some problems with 4.0 which I couldn't fix right away.
It also generates a tonemapped jpg, just for fun. Haven't been able to get this jpg to show up in the theta ios app. Don't know why maybe someone can help?
If you want to build it for yourself make sure to change the file paths in the Android.mk file (in the app folder).

 
***Credits***

- The picture taking part is largely based on the work of Ichi Hirota’s dual-fisheye plug-in <https://github.com/theta360developers/original-dual-fisheye-plugin>
- The integration of OpenCV is a combination of <https://community.theta360.guide/t/ricoh-blog-post-running-opencv-in-your-ricoh-theta/4084> and <https://www.learn2crack.com/2016/03/setup-opencv-sdk-android-studio.html> and a lot of trail and error!
- The HDR part is based on https://www.learnopencv.com/high-dynamic-range-hdr-imaging-using-opencv-cpp-python/

Feel free to change, improve and of course use!

Let me know what you think and run into!




 * TODO v2
 * - add web interface
 * - turn sound on/off
 * - turn iso looping on/off
 * - exr half/full float on/off
 * - download exr
 * - name session
 * - viewer
 * - show status
 * - stops step setting?
 * - number of pics?
 * - dng and exr support

 * TODO ideas
 * - split in two seperate pieces with unstiched version to get seperate crc -> weird crash -> maybe split stiched pic in two?
 * - export default python script to recreate hdri offline?
 * - add dng  to output -> support adobe dng sdk
 * - support opencv 4
 * - fix black hole sun
 * - support Z1
 * - support tonemapped jpg in theta default app -> no idea why it doesn't work, maybe something with adding right exif data but maybe not.
 *

 

 

 

 
