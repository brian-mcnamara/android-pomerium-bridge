# Android Pomerium Bridge

Current work in progress, but the goal is to provide a transparent bridge onto a Pomerium route
without managing authentication at the client level. This allows other apps on the device
to use the local socket as if connecting to the destination without Pomerium.

I'm currently using this for Home Assistant, but any application should theoretically work.