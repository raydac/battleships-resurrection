>Through our GFX technology, Gamefederation offers maximum value to game developers that will translate through the service provider to fulfill end-users' high demand for flexibility and user-friendliness. - Thomas Lindgren, CEO at Gamefederation, 12 June 2001


Just quickly made server from scratch to support multi-room game based on GFX technology, without any extra features,
all Java sources as in 2001.

I have adapted build process for [maven build tool](https://maven.apache.org/) so that just start maven with `mvn` in
the root. If you want start built server just from maven then use `mvn exec:java`

# Historical remark

I was just a bit longer than one year in Java as started the project so that style looks absolutely terrible and bizarre from modern point of view. During development, I decided to use "long POST" operation to read data from the server and reinvented COMET technology, but it was 2001 and who knows, may be I was from the first ones. Markus Persson told me that they very liked such approach and would use it. But such approach didn't work well with mobile operators (as I recall) because many mobile operators made intermediate data buffering until whole stream end.