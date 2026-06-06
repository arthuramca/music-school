package com.arthas.musicschool.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final String DB_PATH = System.getProperty("user.home") + "/music-school/school.db";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public File backupToOneDrive() throws IOException {
        String oneDrive = System.getenv("OneDrive");
        if (oneDrive == null || oneDrive.isBlank()) {
            oneDrive = System.getenv("OneDriveConsumer");
        }
        if (oneDrive == null || oneDrive.isBlank()) {
            throw new IOException("OneDrive não encontrado. Use backup manual.");
        }
        File destDir = new File(oneDrive, "Backups/MusicSchool");
        destDir.mkdirs();
        return copyDb(destDir);
    }

    public File backupToDirectory(File directory) throws IOException {
        return copyDb(directory);
    }

    private File copyDb(File destDir) throws IOException {
        String name = "school_backup_" + LocalDateTime.now().format(FMT) + ".db";
        File dest = new File(destDir, name);
        Files.copy(Paths.get(DB_PATH), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }
}
