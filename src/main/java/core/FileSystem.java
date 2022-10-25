/*
 * Name: Sri Harish Pinnimti
 * UTA ID: 1001865949
 * Net ID: sxp5949
 *
 */
package core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

public class FileSystem {
    final String directoryPath;
    private ConcurrentHashMap<String, FileMeta> cachedFiles;

    public FileSystem(String directoryPath) {
        this.directoryPath = directoryPath;
        this.reloadCachedFiles();
    }

    /**
     * Invalidates the cache and fetches new list from the file system.
     * This has to be called explicitly to update the cache of stored files.
     */
    public synchronized void reloadCachedFiles() {
        this.cachedFiles = new ConcurrentHashMap<>();

        for(FileMeta s: this.listFiles())
            this.cachedFiles.put(s.name, s);
    }

    /**
     * Add a server file to the cache individually.
     * @param file file info to add to cache
     */
    public void addFileToCache(FileMeta file) { this.cachedFiles.put(file.name, file);}

    /**
     * Remove a file info from the cache individually.
     * @param file file info to delete from cache
     */
    public void deleteFileFromCache(FileMeta file) { this.cachedFiles.remove(file.name); }

    /**
     * Returns the cached files
     * @return cached files as a List
     */
    public List<FileMeta> getCachedFiles() {return this.cachedFiles.values().stream().toList();}

    /**
     * Searches in the directory for files and returns them
     * @return List of ServerFile objects
     */
    public List<FileMeta> listFiles() { return listFiles(this.directoryPath); }

    /**
     * Searches in the specified directory for files and returns them
     * @param directoryPath directory name to search
     * @return List of file info objects
     */
    public static List<FileMeta> listFiles(String directoryPath) {
        List<FileMeta> fileList = new ArrayList<>();

        // Gets list of all files and directories present in the `directoryPath`
        File[] files = new File(directoryPath).listFiles();

        // If there are no files in the directory, it returns null
        if(files == null) return fileList;

        for(File f: files) {
            // If it is not a file, skip it
            if(!f.isFile()) continue;
            // Ignore hidden files
            if(f.getName().startsWith(".")) continue;
            // If it is a file, add it to the list
            fileList.add(new FileMeta(f.getName(), f.length(), f.lastModified()));
        }
        return fileList;
    }

    /**
     * Scans the directory and returns the list of deleted files based on previous cached files.
     * @return List of deleted files
     */
    public List<FileMeta> getDeletedFiles() {
        Set<String> cachedSet = new HashSet<>(this.cachedFiles.keySet());
        Set<String> newSet = new HashSet<>(this.listFiles().stream().map(x -> x.name).toList());

        // Set operation: (A - B)
        cachedSet.removeAll(newSet);
        // Filters cachedFiles and returns all the files present in the cachedSet
        return this.cachedFiles.values().stream().filter((x) -> cachedSet.contains(x.name)).map(x -> {
            x.operation = FileOperation.DELETE;
            x.lastModifiedEpoch = System.currentTimeMillis();
            return x;
        }).toList();
    }

    /**
     * Scans the directory and returns the list of added files based on previous cached files.
     * @return List of added files
     */
    public List<FileMeta> getAddedFiles() {
        Set<String> cachedSet = new HashSet<>(this.cachedFiles.keySet());
        List<FileMeta> newList = this.listFiles();
        Set<String> newSet = new HashSet<>(newList.stream().map(x -> x.name).toList());

        // Set Operation: (A - B)
        newSet.removeAll(cachedSet);
        return newList.stream().filter((x) -> newSet.contains(x.name)).peek(x -> x.operation = FileOperation.CREATE).toList();
    }

    /**
     * Scans the directory and returns the list of modified files based on previous cached files.
     * @return List of modified files
     */
    public List<FileMeta> getModifiedFiles() {
        Map<String, FileMeta> newMap = new HashMap<>();

        for(FileMeta s: this.listFiles())
            newMap.put(s.name, s);

        // The name should be same but the hashcode must be different.
        return newMap.values().stream().filter(x -> this.cachedFiles.containsKey(x.name) && x.hashCode() != this.cachedFiles.get(x.name).hashCode()).peek(x -> x.operation = FileOperation.UPDATE).toList();
    }

//    /**
//     * Scans the directory and returns the list of newer version files based on previous cached files last modified.
//     * @param otherVersionFiles Map of other version files
//     * @return List of newer version files
//     */
//    public List<FileMeta> getNewVersionFiles(Map<String, FileMeta> otherVersionFiles) {
//        Set<String> newSet = new HashSet<>(otherVersionFiles.keySet());
//        Set<String> oldSet = new HashSet<>(this.cachedFiles.keySet());
//
//        oldSet.removeAll(newSet); // A - B
//        newSet.retainAll(this.cachedFiles.keySet()); // A n B
//
//        // The file is present in both sets and the older version file is compared with newer version file's last modified
//        return this.cachedFiles.values().stream().filter(x -> {
//            return oldSet.contains(x.name) ||
//                    (newSet.contains(x.name) && otherVersionFiles.get(x.name).lastModifiedEpoch < x.lastModifiedEpoch);
//        }).toList();
//
//    }
//
//    /**
//     * Scans the directory and returns the list of newer version files based on previous cached files last modified.
//     * @param newFiles List of other version files
//     * @return List of newer version files
//     */
//    public List<FileMeta> getNewVersionFiles(List<FileMeta> newFiles) {
//        Map<String, FileMeta> map = new HashMap<>();
//
//        for(FileMeta s: newFiles)
//            map.put(s.name, s);
//
//        return this.getNewVersionFiles(map);
//    }

    public synchronized void writeToDisk(byte[] data, FileMeta fileMeta) throws IOException {
        // Write the data to the file.
        File file = new File(this.directoryPath + "/" + fileMeta.name);
//        synchronized (file) {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(data);
            bos.flush();
            bos.close();
            file.setLastModified(fileMeta.lastModifiedEpoch);
//        }
    }

    public synchronized byte[] readFromDisk(String fileName) throws IOException {
        // Read the data from the file.
        File file = new File(this.directoryPath + "/" + fileName);
        //        synchronized (file) {
        if (!file.exists()) return new byte[0];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(this.directoryPath + "/" + fileName));
        byte[] data = bis.readAllBytes();
        bis.close();
//        }
        return data;
    }

    public synchronized void deleteFromDisk(FileMeta fileMeta)  {
        // Delete the file.
        File file = new File(this.directoryPath + "/" + fileMeta.name);
        if(file.exists()) file.delete();
    }
}
