// import 'package:firebase_auth/firebase_auth.dart';
// import 'package:http/http.dart' as http;
import 'package:e3kit/e3kit.dart';
import 'package:flutter/services.dart';

// import 'dart:convert';
// import 'dart:io' show Platform;
import 'package:cloud_functions/cloud_functions.dart';

class Device {
  EThree eThree;
  String identity;

  Device(this.identity);

  initialize() async {
    Future tokenCallback() async {
      final HttpsCallable callable = CloudFunctions.instance
        .getHttpsCallable(functionName: 'getVirgilJwt');
      final data = (await callable.call()).data;
      print("retrieved Json Web Token from server");
      return data["token"];
    }
    try {
      //# start of snippet: e3kit_initialize
      this.eThree = await EThree.init(identity, tokenCallback);
      //# end of snippet: e3kit_initialize
      print('Initialized');
    } catch(err) {
      print('Failed initializing: $err');
    }
  }

  EThree getEThree() {
    if (this.eThree == null) {
      throw 'eThree not initialized for $identity';
    }

    return this.eThree;
  }

  register() async {
    final eThree = getEThree();

    if(await eThree.hasLocalPrivateKey()){
      print('User already signed in');
    }else{
      try {
        print('trying to register');
        await eThree.register();
        print('Registered');
        await eThree.backupPrivateKey("password");
        print('Backed up');
      } on PlatformException catch(err) { 
        print('Failed registering: ${err.message}, ${err.details}'); 
        if (err.message == '70107: User is already registered.') {
          print('Attempting to restore private key..');
          await restorePrivateKey("password");
          print('User signed in');
        }
      }
    }
  }


  findUsers(List<String> identities) async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_find_users
      final result = await eThree.findUsers(identities);
      //# end of snippet: e3kit_find_users
      print('Looked up $identities\'s public key');
      return result;
    } catch(err) {
      print('Failed looking up $identities\'s public key: $err');
    }
  }

  encrypt(text, [users]) async {
    final eThree = getEThree();

    String encryptedText;

    try {
      //# start of snippet: e3kit_sign_and_encrypt
      encryptedText = await eThree.encrypt(text, users);
      //# end of snippet: e3kit_sign_and_encrypt
      print('Encrypted and signed: \'$encryptedText\'.');
    } catch(err) {
      print('Failed encrypting and signing: $err');
    }

    return encryptedText;
  }

  decrypt(text, [String user]) async {
    final eThree = getEThree();

    String decryptedText;

    try {
      //# start of snippet: e3kit_decrypt_and_verify
      decryptedText = await eThree.decrypt(text, user).timeout(Duration(seconds:10));
      //# end of snippet: e3kit_decrypt_and_verify
      print('Decrypted and verified: \'$decryptedText');
    } catch(err) {
      print('Failed decrypting and verifying: $err');
    }

    return decryptedText;
  }

  backupPrivateKey(String password) async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_backup_private_key
      await eThree.backupPrivateKey(password);
      //# end of snippet: e3kit_backup_private_key
      print('Backed up private key');
    } on PlatformException catch(err) {
      print('Failed backing up private key: $err');
      if (err.code == 'entry_already_exists') {
        await eThree.resetPrivateKeyBackup();
        print('Reset private key backup. Trying again...');
        await backupPrivateKey(password);
      }
    }
  }

  changePassword(String oldPassword, String newPassword) async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_change_password
      await eThree.changePassword(oldPassword, newPassword);
      //# end of snippet: e3kit_change_password
      print('Changed password');
    } on PlatformException catch(err) {
      print('Failed changing password: $err');
    }
  }

  restorePrivateKey(String password) async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_restore_private_key
      await eThree.restorePrivateKey(password);
      //# end of snippet: e3kit_restore_private_key
      print('Restored private key');
    } on PlatformException catch(err) {
      print('Failed restoring private key: $err');
      if (err.code == 'keychain_error') {
        await eThree.cleanUp();
        print('Cleaned up. Trying again...');
        await restorePrivateKey(password);
      }
    }
  }

  resetPrivateKeyBackup() async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_reset_private_key_backup
      await eThree.resetPrivateKeyBackup();
      //# end of snippet: e3kit_reset_private_key_backup
      print('Reset private key backup');
    } on PlatformException catch(err) {
      print('Failed resetting private key backup: $err');
    }
  }

  rotatePrivateKey() async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_rotate_private_key
      await eThree.rotatePrivateKey();
      //# end of snippet: e3kit_rotate_private_key
      print('Rotated private key');
    } on PlatformException catch(err) {
      print('Failed rotating private key: $err');
      if (err.code == 'private_key_exists') {
        await eThree.cleanUp();
        print('Cleaned up. Trying again...');
        await rotatePrivateKey();
      }
    }
  }

  cleanUp() async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_cleanup
      await eThree.cleanUp();
      //# end of snippet: e3kit_cleanup
      print('Cleaned up');
    } on PlatformException catch(err) {
      print('Failed cleaning up: $err');
    }
  }

  unregister() async {
    final eThree = getEThree();

    try {
      //# start of snippet: e3kit_unregister
      await eThree.unregister();
      //# end of snippet: e3kit_unregister
      print('Unregistered');
    } on PlatformException catch(err) {
      print('Failed unregistering: $err');
    }
  }

  createRatchetChannel(String identity) async {
    final eThree = getEThree();
    try{
      await eThree.createRatchetChannel(identity);
      print("Ratchet channel created");
    } on PlatformException catch (err) {
      print("Ratchet channel creation failed: $err");
    }
  }

  joinRatchetChannel(String identity) async {
    final eThree = getEThree();
    try{
      await eThree.joinRatchetChannel(identity);
      print("Ratchet channel joined");
    } on PlatformException catch (err) {
      print("Ratchet channel join failed: $err");
    }
  }

  Future<bool> hasRatchetChannel(String identity) async {
    final eThree = getEThree();
    try{
      final channelExists = await eThree.hasRatchetChannel(identity);
      if(channelExists){
        print("Ratchet channel exists in local storage");
      }else{
        print("Ratchet channel does not exist in local storage");
      }
      return channelExists;
    } on PlatformException catch (err) {
      print("Ratchet channel does not exist in local storage: $err");
      return false;
    }
  }

  getRatchetChannel(String identity) async {
    final eThree = getEThree();
    try{
      final getSuccess = await eThree.getRatchetChannel(identity);
      if(getSuccess){
        print("Get ratchet channel successfully");
      }else{
        print("Get ratchet channel failed");
      }
    } on PlatformException catch (err) {
      print("Failed to get ratchet channel: $err");
    }
  }

  Future<String> ratchetEncrypt(String identity, String message) async {
    final eThree = getEThree();
    try{
      final String encrypted = await eThree.ratchetEncrypt(identity, message);
      print("double ratchet encryption succeeded");
      return encrypted;
    } on PlatformException catch (err) {
      print("double ratchet encryption failed: $err");
      return null;
    }
  }

  Future<String> ratchetDecrypt(String identity, String message) async {
    final eThree = getEThree();
    try{
      final String decrypted = await eThree.ratchetDecrypt(identity, message);
      print("double ratchet decryption succeeded");
      return decrypted;
    } on PlatformException catch (err) {
      print("double ratchet decryption failed: $err");
      return null;
    }
  }

  Future<void> deleteRatchetChannel(String identity) async {
    final eThree = getEThree();
    try{
      eThree.deleteRatchetChannel(identity);
      print("delete ratchet channel success");
    } on PlatformException catch (err) {
      print("delete ratchet channel failed: $err");
      return null;
    }
  }

}