Puzzle

Android Puzzle Game that uses built in images Folder 

Project Location /home/jay/Documents/Scripts/AI/OpenCode/Android-Apps/Puzzle/

Location for the built in puzzle images /home/jay/Documents/Scripts/AI/OpenCode/Android-Apps/Puzzle/images/

Allow to choose 1 of the built in images to use as a puzzle
Allow upload of a users own image to play as a puzzle
Add a Random Puzzle button so it turns 1 of the built in images into a puzzle and starts the puzzel game
Allow settings to choose the grid size of the puzzel, timer settings (toggled off by default).
Must be able to work with different size images
will have a mix of portrait and landscape orientation (auto adjust for image size and orentation)
allow manual resizing using finger and thumb pinching (try auto fit image with padding space around edges (try not to stretch image too much to keep quality high as possible)
use all the images in images folder location (theres around 96 jpg images)
allow user to select Favourite puzzle images, allow user to chopose what puzzle images they dont want to see aswell in Settings Show / Hide option.

Remember: This projewct is nothing to do with any other project or app especailly checkers app so keep this app seperate completely!

Use github workflows to build the app (and any update to it) and put finally release and any update for the app and place in  apk folder in the project location aswell as backup known working versions in Backup folder before doing major updates

Dont edit this prompt file

Never change anything in Backup folders (if it exists unlessyou backing up a current working version before making a major update or upgrade) but you can use them as a read-only reference if a mistake is made and you need to fix something or restore previously working versions

Save changes to file(s) in question

Then after files are added / edited then save any changes made to changes.txt

Implement persistent error handling and debugging throughout the project. Every failure, exception, or unexpected state should generate a clear error code, detailed debug output, and useful diagnostic information to help identify the exact cause quickly.

Do not remove debugging systems after issues are fixed — keep all error codes, logging, stack traces, validation checks, and diagnostic tools permanently integrated so that any future bugs, crashes, or unexpected behaviour can be traced and resolved efficiently.

Always use same key-store for each app made via github workflows so it can update correctly without requiring uninstallation

Save changes to changes.txt (create if not exists)

Tell me when ready to test (stay quiet after acknowledging you got the message / request / mission every time and stay quiet till its ready to test and respond only if fully complete  or if you need input from me or if I ask for an update)!

When giving final github release link (where applicable), make sure it points to the newest release but without the tag or filename so I can see the correct location without direct downloading the file as thats best practice!

Each app needs an About section showing
In about section it should say Made by jnetai.com 
The full version number (same as github release version tag) also add a Check for update button (so internet permissions required) to check latest release version (tag in full)
Add a Share App button so users can share the app.
 
Each update should use same key store so the app can update and not require uninstall of the app to update it.

Each app should have its own local folder and own github repository and own keystore that remains the same so it can update without uninstall 1st and be dark centered themed and allow space at bottom so buttons or elements at the bottom of the app should not be cut off, it should look professional.

App compatibility: apps needs to work on samsung s8 and onwards and google pixel 6 and onwards
full path to Downloads is /storage/emulated/0/Download/ (called Downloads as an alias in android)

In releases on github a meaningful name should be used for example Tetris.apk (no need for a debug version of any app or game for android just put the debug version as the main version!

Github api tokens / passwords etc can be found in /home/jay/Documents/Scripts/AI/openclaw/password-vault/
also can be found in /home/jay/Documents/Scripts/AI/OpenCode/password-vault/

Prime Directive is always build on github never on the pi (do it remotely with github never locally)
If a check for update ever fails in the app it should fallover to just opening link to latest github repo for this app.

Build the releases via github actions / workflows (not locally ever on pi (remember your prime directive)) in there own repository (1 per app and auto incriment versions in case user wants to ever go back)

Start now with all in order no questions asked!

