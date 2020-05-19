
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:firebase_auth/firebase_auth.dart';

import 'device.dart';

class TestingPage extends StatefulWidget {
  TestingPage();
  @override
  _TestingPageState createState() => _TestingPageState();
}

class _TestingPageState extends State<TestingPage> {

  Device alice;
  final String oppUid = "DlzIj2dikgNwtF2eXoJCKGogXGE2";

  // authentication
  GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: [
      'email',
      'profile',
    ],
  );

  initializeUser() async {
    try {
      await _googleSignIn.signIn();
      print("signed in");
    } catch (error) {
      print(error);
    }
  }

  @override
  void initState() {
    super.initState();
    _googleSignIn.onCurrentUserChanged.listen((GoogleSignInAccount account) async {
      if (account != null) {
        final credentials = await account.authentication;
        FirebaseAuth.instance.signInWithCredential(GoogleAuthProvider.getCredential(
          idToken: credentials.idToken,
          accessToken: credentials.accessToken
        )).then((authResult) async {
          print("${authResult.user.displayName} sign in, uid ${authResult.user.uid}");
          alice = Device(authResult.user.uid);
          await alice.initialize();
        });
      }
    });
  }

  @override
  Widget build(BuildContext context){
    return Scaffold(
        appBar: AppBar(title: const Text("Testing e3kit")),
        body: Padding(
          padding: EdgeInsets.all(10),
          child: ListView(
            children: <Widget>[
              RaisedButton(
                child: Text("Initialize user"),
                onPressed: initializeUser,),
              RaisedButton(
                child: Text("Register user"),
                onPressed: () async { 
                  await alice.register(); 
                }),
              RaisedButton(
                child: Text("Find another user"),
                onPressed: () async { await alice.findUsers([oppUid]); }),
              RaisedButton(
                child: Text("Default encrypt and decrypt"),
                onPressed: () async { 
                  final String encrypted = await alice.encrypt("default encrypt"); 
                  final String decrypted = await alice.decrypt(encrypted); 
                  if(decrypted == "default encrypt"){
                    print("decryption successful");
                  }else{
                    print("decryption failed");
                  }
                }),
              RaisedButton(
                child: Text("Check if ratchet channel exists"),
                onPressed: () async { 
                  try{
                    if(await alice.hasRatchetChannel(oppUid)){
                      return;
                    }else{
                      try{
                        await alice.getRatchetChannel(oppUid);
                      }catch(e){
                        print("error getting ratchet channel $e");
                      }
                    }
                  }catch(e){
                    print("error checking ratchetChannel hashmap $e");
                  }
                }),
              RaisedButton(
                child: Text("Create ratchet channel"),
                onPressed: () async { await alice.createRatchetChannel(oppUid); }),
              RaisedButton(
                child: Text("Join ratchet channel"),
                onPressed: () async { await alice.joinRatchetChannel(oppUid); }),
              RaisedButton(
                child: Text("Ratchet encrypt and decrypt"),
                onPressed: () async { 
                  final String encrypted = await alice.ratchetEncrypt(oppUid, "ratchet encrypt"); 
                  final String decrypted = await alice.ratchetDecrypt(oppUid, encrypted);
                  if(decrypted == "ratchet encrypt"){
                    print("decryption successful");
                  }else{
                    print("decryption failed");
                  }
                }),
              RaisedButton(
                child: Text("Delete ratchet channel"),
                onPressed: () async { await alice.deleteRatchetChannel(oppUid); }),
            ],
          ),
        ),
        bottomNavigationBar: RaisedButton(
          child: Text("Sign out"),
          onPressed: () async {
            await alice.cleanUp();
            await _googleSignIn.signOut();
          },
        ),
    );
  }
}