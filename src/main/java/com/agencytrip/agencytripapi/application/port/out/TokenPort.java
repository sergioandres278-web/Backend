package com.agencytrip.agencytripapi.application.port.out;
public interface TokenPort { String create(int userId, String name, String email, String role); }
