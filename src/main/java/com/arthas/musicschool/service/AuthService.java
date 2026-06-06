package com.arthas.musicschool.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {

    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), "music-school");
    private static final Path AUTH_FILE  = CONFIG_DIR.resolve("auth.json");
    private static final Gson GSON       = new GsonBuilder().setPrettyPrinting().create();

    private record Credentials(String username, String passwordHash) {}

    public boolean isFirstRun() {
        return !Files.exists(AUTH_FILE);
    }

    public void setupInitialCredentials(String username, String password) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Files.writeString(AUTH_FILE, GSON.toJson(new Credentials(username, hash(password))));
    }

    public boolean authenticate(String username, String password) {
        try {
            if (!Files.exists(AUTH_FILE)) return false;
            Credentials creds = GSON.fromJson(Files.readString(AUTH_FILE), Credentials.class);
            return creds.username().equals(username) && creds.passwordHash().equals(hash(password));
        } catch (Exception e) {
            return false;
        }
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
