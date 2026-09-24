package com.agencytrip.agencytripapi.application.port.out;
public interface PasswordHasherPort { String hash(String plain); boolean matches(String plain, String hash); }
