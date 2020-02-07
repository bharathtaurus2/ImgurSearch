# ImgurSearch

A client application to search for images on imgur.com (written in kotlin)

The application's features are described below:
* Images are displayed upon the user entering a search text
* The app displays top images of the week based on the search text
* Images are displayed as a list of cards which contain the following additional info:
  -> title
  -> date of post in local time (DD/MM/YYYY h:mm a)
  -> number of additional images in post (if there are multiple)
  
* If a search result contains multiple images (of an album), it will display only the cover image of the album.
* There is a toggle switch to change the display results.
  -> If toggle is enabled, the app will only display results where the sum of “points”, “score” and “topic_id” of the post adds up to an even number
  -> If the toggle is disabled, the app will display all results.
 
Instructions on using the app:
* Clone the project using git or download a zip file of the project and open it in Android Studio.
* The external Libraries used in the project are below:
  -> Picasso: com.squareup.picasso:picasso:2.71828
  -> OkHttp3: com.squareup.okhttp3:okhttp:4.3.1
  -> Kotlin Reflection: org.jetbrains.kotlin:kotlin-reflect:$kotlin_version

* The app access the Imgur API which uses the OAuth2 protocol. To provision this, the project is configured to read the Client-ID needed for authentication, from a properties file named 'imgurauth.properties', which is not checked-in to the repository.
Once this project is downloaded or synced, the 'imgurauth.properties' file needs to be created in the root directory of the project and must contain the following line:
Client-ID="Replace with your client ID"

For instrcutions on getting a Client ID to access imgur.com, please refer to the documentation at https://apidocs.imgur.com/?version=latest
