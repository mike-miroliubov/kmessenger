package com.kite.kmessenger.exception

class UserNotFoundException(username: String) : Exception("User $username does not exist")