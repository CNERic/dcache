package org.dcache.chimera.nfs;

import diskCacheV111.util.AccessLatency;
import diskCacheV111.util.RetentionPolicy;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.dcache.chimera.ChimeraFsException;
import org.dcache.chimera.DirectoryStreamB;
import org.dcache.chimera.FileSystemProvider;
import org.dcache.chimera.FsInode;
import org.dcache.chimera.FsStat;
import org.dcache.chimera.HimeraDirectoryEntry;
import org.dcache.chimera.IOHimeraFsException;
import org.dcache.chimera.StorageLocatable;
import org.dcache.chimera.posix.Stat;
import org.dcache.chimera.store.InodeStorageInformation;
import org.dcache.acl.ACE;
import org.dcache.chimera.UnixPermission;
import org.dcache.chimera.nfs.v4.xdr.nfsstat4;
import org.dcache.xdr.RpcCall;

/**
 * A decorator implementation of {@link FileSystemProvider} which sits on top of
 * real filesystem and manages extra properties of file system objects like
 * export access control or referrals.
 *
 * Chimera inode contain filesystem id. The PseudoFsProvider changes id to indicate
 * pseudo filesyste inode, nevertheless the id of real inode is used. This allows
 * to have persistent file handles and survive reboots (restarts).
 *
 * @since 2.1
 */
public class PseudoFsProvider implements FileSystemProvider {

    private final FileSystemProvider _inner;
    private final ExportFile _export;
    private final RpcCall _call;

    private final int PSEUDO_FS_ID = 255;
    private final byte[] PSEUDO_FH_SIGNATURE = Integer.toString(PSEUDO_FS_ID)
            .getBytes(Charset.forName("utf8"));

    private static final ConcurrentMap<FsInode, PseudoFsNode> EXPORT_CACHE =
            new ConcurrentHashMap<FsInode, PseudoFsNode>();

    public PseudoFsProvider(FileSystemProvider inner, ExportFile exportFile, RpcCall call) {
        _inner = inner;
        _export = exportFile;
        _call = call;
    }

    @Override
    public void addInodeLocation(FsInode inode, int type, String location) throws ChimeraFsException {
        _inner.addInodeLocation(inode, type, location);
    }

    @Override
    public void clearInodeLocation(FsInode inode, int type, String location) throws ChimeraFsException {
        _inner.clearInodeLocation(inode, type, location);
    }

    @Override
    public FsInode createFile(String path) throws ChimeraFsException {
        return _inner.createFile(path);
    }

    @Override
    public FsInode createFile(FsInode parent, String name) throws ChimeraFsException {
        return _inner.createFile(parent, name);
    }

    @Override
    public FsInode createFile(FsInode parent, String name, int owner, int group, int mode) throws ChimeraFsException {
        return _inner.createFile(parent, name, owner, group, mode);
    }

    @Override
    public FsInode createFile(FsInode parent, String name, int owner, int group, int mode, int type) throws ChimeraFsException {
        return _inner.createFile(parent, name, owner, group, mode, type);
    }

    @Override
    public FsInode createFileLevel(FsInode inode, int level) throws ChimeraFsException {
        return _inner.createFileLevel(inode, level);
    }

    @Override
    public void createFileWithId(FsInode parent, FsInode inode, String name, int owner, int group, int mode, int type) throws ChimeraFsException {
        _inner.createFileWithId(parent, inode, name, owner, group, mode, type);
    }

    @Override
    public FsInode createHLink(FsInode parent, FsInode inode, String name) throws ChimeraFsException {
        return _inner.createHLink(parent, inode, name);
    }

    @Override
    public FsInode createLink(String src, String dest) throws ChimeraFsException {
        return _inner.createLink(src, dest);
    }

    @Override
    public FsInode createLink(FsInode parent, String name, String dest) throws ChimeraFsException {
        return _inner.createLink(parent, name, dest);
    }

    @Override
    public FsInode createLink(FsInode parent, String name, int uid, int gid, int mode, byte[] dest) throws ChimeraFsException {
        return _inner.createLink(parent, name, uid, gid, mode, dest);
    }

    @Override
    public void createTag(FsInode inode, String name) throws ChimeraFsException {
        _inner.createTag(inode, name);
    }

    @Override
    public void createTag(FsInode inode, String name, int uid, int gid, int mode) throws ChimeraFsException {
        _inner.createTag(inode, name, uid, gid, mode);
    }

    @Override
    public AccessLatency getAccessLatency(FsInode inode) throws ChimeraFsException {
        return _inner.getAccessLatency(inode);
    }

    @Override
    public int getFsId() {
        return PSEUDO_FS_ID;
    }

    @Override
    public FsStat getFsStat() throws ChimeraFsException {
        return _inner.getFsStat();
    }

    @Override
    public String getInfo() {
        return _inner.getInfo();
    }

    @Override
    public String getInodeChecksum(FsInode inode, int type) throws ChimeraFsException {
        return _inner.getInodeChecksum(inode, type);
    }

    @Override
    public List<StorageLocatable> getInodeLocations(FsInode inode, int type) throws ChimeraFsException {
        return _inner.getInodeLocations(inode, type);
    }

    @Override
    public FsInode getParentOf(FsInode inode) throws ChimeraFsException {
        return _inner.getParentOf(inode);
    }

    @Override
    public RetentionPolicy getRetentionPolicy(FsInode inode) throws ChimeraFsException {
        return _inner.getRetentionPolicy(inode);
    }

    @Override
    public InodeStorageInformation getSorageInfo(FsInode inode) throws ChimeraFsException {
        return _inner.getSorageInfo(inode);
    }

    @Override
    public int getTag(FsInode inode, String tagName, byte[] data, int offset, int len) throws ChimeraFsException {
        return _inner.getTag(inode, tagName, data, offset, len);
    }

    @Override
    public String inode2path(FsInode inode) throws ChimeraFsException {
        return _inner.inode2path(inode);
    }

    @Override
    public String inode2path(FsInode inode, FsInode startFrom, boolean inclusive) throws ChimeraFsException {
        return _inner.inode2path(inode, startFrom, inclusive);
    }

    @Override
    public FsInode inodeOf(FsInode parent, String name) throws ChimeraFsException {

        /*
         * if inode is a pseudofs inode check for a mount point.
         * The first mountpoint will win if allowed. If we reach
         * the end of pseudo fs and there are no allowed mount points
         * return ERR_ACCESS to the client.
         *
         * Pseudo inodes use real inode ids but encode pseudo fs id into
         * nfs file handle. This allowes to have permanent file handles.
         *
         */
        FsInode inode = _inner.inodeOf(parent, name);
        if (isPseudoFs(parent)) {
            PseudoFsNode parentNode = EXPORT_CACHE.get(parent);

            PseudoFsNode child = parentNode.getNode(name);
            if(child.isMountPoint()) {
                if(child.getExport().isAllowed(remoteAddressOf(_call))) {
                    /*
                     * we hit the first allowed mountpoint.
                     * This have to be enough to give client access.
                     */
                    return makeRealInode(inode);
                }else{
                    if(child.isLeaf()) {
                        /*
                         * the end of pseudo fs brach, no allowed mount points.
                         */
                        throw new ChimeraNFSException(nfsstat4.NFS4ERR_ACCESS, "permission denied");
                    }
                }
            }

            /*
             * turn inode into pseudo inode
             */
            inode = new PseudoInode(this, inode.toString());
            EXPORT_CACHE.putIfAbsent(inode, child);
        }
        return inode;
    }

    @Override
    public boolean isIoEnabled(FsInode inode) throws ChimeraFsException {
        return _inner.isIoEnabled(inode);
    }

    @Override
    public FsInode mkdir(String path) throws ChimeraFsException {
        return _inner.mkdir(path);
    }

    @Override
    public FsInode mkdir(FsInode parent, String name) throws ChimeraFsException {
        return _inner.mkdir(parent, name);
    }

    @Override
    public FsInode mkdir(FsInode parent, String name, int owner, int group, int mode) throws ChimeraFsException {
        return _inner.mkdir(parent, name, owner, group, mode);
    }

    @Override
    public boolean move(String source, String dest) {
        return _inner.move(source, dest);
    }

    @Override
    public boolean move(FsInode srcDir, String source, FsInode destDir, String dest) throws ChimeraFsException {
        return _inner.move(srcDir, source, destDir, dest);
    }

    @Override
    public DirectoryStreamB<HimeraDirectoryEntry> newDirectoryStream(FsInode dir) throws IOHimeraFsException {

        /*
         * List on a pseudo fs will return entries which exists.
         * We filter out all entries this are not included in export.
         *
         * TODO: filter out all entries which are not allowed for this client
         */
        DirectoryStreamB<HimeraDirectoryEntry> directoryEntries =
                _inner.newDirectoryStream(dir);
        if(isPseudoFs(dir)) {
            PseudoFsNode dirNode = EXPORT_CACHE.get(dir);
            if(!dirNode.isMountPoint()) {
                Set<String> dirs = new HashSet<String>();
                for( PseudoFsNode child: dirNode.getChildren()) {
                    dirs.add(child.getName());
                }
                return new PsudoFsDirectoryStream(directoryEntries, dirs);
            }
        }
        return directoryEntries;
    }

    @Override
    public FsInode path2inode(String path) throws ChimeraFsException {
        /*
         * The root of the file system is always on pseudo fs.
         */
        FsInode inode = _inner.path2inode(path);
        if (path.equals("/")) {
            FsInode rootInode = makePseudoInode(new FsInode(this, inode.toString()));
            EXPORT_CACHE.putIfAbsent(rootInode, _export.getPseuFsRoot());
            return rootInode;
        }
        return inode;
    }

    @Override
    public FsInode path2inode(String path, FsInode startFrom) throws ChimeraFsException {
        return _inner.path2inode(path, startFrom);
    }

    @Override
    public int read(FsInode inode, int level, long beginIndex, byte[] data, int offset, int len) throws ChimeraFsException {
        return _inner.read(inode, level, beginIndex, data, offset, len);
    }

    @Override
    public byte[] readLink(String path) throws ChimeraFsException {
        return _inner.readLink(path);
    }

    @Override
    public byte[] readLink(FsInode inode) throws ChimeraFsException {
        return _inner.readLink(inode);
    }

    @Override
    public boolean remove(String path) throws ChimeraFsException {
        return _inner.remove(path);
    }

    @Override
    public boolean remove(FsInode parent, String name) throws ChimeraFsException {
        return _inner.remove(parent, name);
    }

    @Override
    public boolean remove(FsInode inode) throws ChimeraFsException {
        return _inner.remove(inode);
    }

    @Override
    public boolean removeFileMetadata(String path, int level) throws ChimeraFsException {
        return _inner.removeFileMetadata(path, level);
    }

    @Override
    public void removeInodeChecksum(FsInode inode, int type) throws ChimeraFsException {
        _inner.removeInodeChecksum(inode, type);
    }

    @Override
    public void removeTag(FsInode dir, String tagName) throws ChimeraFsException {
        _inner.removeTag(dir, tagName);
    }

    @Override
    public void removeTag(FsInode dir) throws ChimeraFsException {
        _inner.removeTag(dir);
    }

    @Override
    public void setAccessLatency(FsInode inode, AccessLatency accessLatency) throws ChimeraFsException {
        _inner.setAccessLatency(inode, accessLatency);
    }

    @Override
    public void setFileATime(FsInode inode, long atime) throws ChimeraFsException {
        _inner.setFileATime(inode, atime);
    }

    @Override
    public void setFileATime(FsInode inode, int level, long atime) throws ChimeraFsException {
        _inner.setFileATime(inode, level, atime);
    }

    @Override
    public void setFileCTime(FsInode inode, long ctime) throws ChimeraFsException {
        _inner.setFileCTime(inode, ctime);
    }

    @Override
    public void setFileCTime(FsInode inode, int level, long ctime) throws ChimeraFsException {
        _inner.setFileCTime(inode, level, ctime);
    }

    @Override
    public void setFileGroup(FsInode inode, int newGroup) throws ChimeraFsException {
        _inner.setFileGroup(inode, newGroup);
    }

    @Override
    public void setFileGroup(FsInode inode, int level, int newGroup) throws ChimeraFsException {
        _inner.setFileGroup(inode, level, newGroup);
    }

    @Override
    public void setFileMTime(FsInode inode, long mtime) throws ChimeraFsException {
        _inner.setFileMTime(inode, mtime);
    }

    @Override
    public void setFileMTime(FsInode inode, int level, long mtime) throws ChimeraFsException {
        _inner.setFileMTime(inode, level, mtime);
    }

    @Override
    public void setFileMode(FsInode inode, int newMode) throws ChimeraFsException {
        _inner.setFileMode(inode, newMode);
    }

    @Override
    public void setFileMode(FsInode inode, int level, int newMode) throws ChimeraFsException {
        _inner.setFileMode(inode, level, newMode);
    }

    @Override
    public void setFileName(FsInode dir, String oldName, String newName) throws ChimeraFsException {
        _inner.setFileName(dir, oldName, newName);
    }

    @Override
    public void setFileOwner(FsInode inode, int newOwner) throws ChimeraFsException {
        _inner.setFileOwner(inode, newOwner);
    }

    @Override
    public void setFileOwner(FsInode inode, int level, int newOwner) throws ChimeraFsException {
        _inner.setFileOwner(inode, level, newOwner);
    }

    @Override
    public void setFileSize(FsInode inode, long newSize) throws ChimeraFsException {
        _inner.setFileSize(inode, newSize);
    }

    @Override
    public void setInodeAttributes(FsInode inode, int level, Stat stat) throws ChimeraFsException {
        _inner.setInodeAttributes(inode, level, stat);
    }

    @Override
    public void setInodeChecksum(FsInode inode, int type, String checksum) throws ChimeraFsException {
        _inner.setInodeChecksum(inode, type, checksum);
    }

    @Override
    public void setInodeIo(FsInode inode, boolean enable) throws ChimeraFsException {
        _inner.setInodeIo(inode, enable);
    }

    @Override
    public void setRetentionPolicy(FsInode inode, RetentionPolicy retentionPolicy) throws ChimeraFsException {
        _inner.setRetentionPolicy(inode, retentionPolicy);
    }

    @Override
    public void setStorageInfo(FsInode inode, InodeStorageInformation storageInfo) throws ChimeraFsException {
        _inner.setStorageInfo(inode, storageInfo);
    }

    @Override
    public int setTag(FsInode inode, String tagName, byte[] data, int offset, int len) throws ChimeraFsException {
        return _inner.setTag(inode, tagName, data, offset, len);
    }

    @Override
    public Stat stat(String path) throws ChimeraFsException {
        return _inner.stat(path);
    }

    @Override
    public Stat stat(FsInode inode) throws ChimeraFsException {
        /*
         * Pseudo inodes directories are read only and shold be not chached.
         * To achive that we return a tweaked stat information.
         */
        if (isPseudoFs(inode)) {
            return psudoInodeStat(inode);
        }
        return _inner.stat(inode);
    }

    @Override
    public Stat stat(FsInode inode, int level) throws ChimeraFsException {

        if (isPseudoFs(inode)) {
            return psudoInodeStat(inode);
        }
        return _inner.stat(inode, level);
    }

    @Override
    public Stat statTag(FsInode dir, String name) throws ChimeraFsException {
        return _inner.statTag(dir, name);
    }

    @Override
    public String[] tags(FsInode inode) throws ChimeraFsException {
        return _inner.tags(inode);
    }

    @Override
    public int write(FsInode inode, int level, long beginIndex, byte[] data, int offset, int len) throws ChimeraFsException {
        return _inner.write(inode, level, beginIndex, data, offset, len);
    }

    @Override
    public void close() throws IOException {
        _inner.close();
    }

    @Override
    public void setACL(FsInode inode, List<ACE> acl) throws ChimeraFsException {
        _inner.setACL(inode, acl);
    }

    @Override
    public List<ACE> getACL(FsInode inode) throws ChimeraFsException {
        return _inner.getACL(inode);
    }

    private boolean isPseudoFs(FsInode inode) {
        return inode.fsId() == PSEUDO_FS_ID;
    }

    private FsInode makePseudoInode(FsInode inode) {
        return new PseudoInode(this, inode.toString());
    }

    private FsInode makeRealInode(FsInode inode) {
        return new FsInode(_inner, inode.toString());
    }
    private static Stat psudoInodeStat(FsInode inode) {
        long now = System.currentTimeMillis();
        Stat stat = new Stat();
        stat.setIno((int) inode.id());
        stat.setUid(0);
        stat.setGid(0);
        stat.setSize(512);
        stat.setMode(0555 | UnixPermission.S_IFDIR);
        stat.setRdev(17);
        stat.setDev(17);
        stat.setATime(now);
        stat.setCTime(now);
        stat.setMTime(now);
        stat.setNlink(2);
        return stat;
    }

    @Override
    public FsInode inodeFromBytes(byte[] bytes) throws ChimeraFsException {
        FsInode inode = _inner.inodeFromBytes(bytes);

        /*
         * if file handle has a pseudo fs signature convert inode
         * info pseudo inode.
         */
        if(verifyHandle(PSEUDO_FH_SIGNATURE, bytes, PSEUDO_FH_SIGNATURE.length))
            return makePseudoInode(inode);
        return inode;
    }

    @Override
    public byte[] inodeToBytes(FsInode inode) throws ChimeraFsException {
        return _inner.inodeToBytes(inode);
    }

    @Override
    public List<FsInode> path2inodes(String path) throws ChimeraFsException {
        return _inner.path2inodes(path);
    }

    @Override
    public List<FsInode> path2inodes(String path, FsInode startFrom) throws ChimeraFsException {
        return _inner.path2inodes(path, startFrom);
    }

    private class PsudoFsDirectoryStream implements DirectoryStreamB<HimeraDirectoryEntry> {

        DirectoryStreamB<HimeraDirectoryEntry> _inner;
        List<HimeraDirectoryEntry> _filteredEntries;
        public PsudoFsDirectoryStream(DirectoryStreamB<HimeraDirectoryEntry> inner, Set<String> filter) {
            _inner = inner;

            _filteredEntries =
                    new ArrayList<HimeraDirectoryEntry>(filter.size());
            for(HimeraDirectoryEntry e : inner) {
                if( filter.contains(e.getName()) )
                    _filteredEntries.add( new HimeraDirectoryEntry(e.getName(),
                            makePseudoInode(e.getInode()),
                            psudoInodeStat(e.getInode())));
            }
        }

        @Override
        public Iterator<HimeraDirectoryEntry> iterator() {
            return _filteredEntries.iterator();
        }

        @Override
        public void close() throws IOException {
            _inner.close();
        }
    }

    private static class PseudoInode extends FsInode {

        public PseudoInode(FileSystemProvider fs, String id) {
            super(fs, id);
        }

        @Override
        public Stat stat() throws ChimeraFsException {
            return PseudoFsProvider.psudoInodeStat(this);
        }

        @Override
        public Stat statCache() throws ChimeraFsException {
            return this.stat();
        }
    }

    private static InetAddress remoteAddressOf(RpcCall call) {
        return call.getTransport().getRemoteSocketAddress().getAddress();
    }

    private static boolean verifyHandle(byte[] original, byte[] bytes, int length) {

        assert original.length >= length;

        if( bytes.length < length)
            return false;

        for (int i = 0; i < length; i++) {
            if (original[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
