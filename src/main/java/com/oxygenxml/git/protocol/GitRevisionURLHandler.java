package com.oxygenxml.git.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.FileHelper;

/**
 * Handler for the "git" protocol. Can be used to for the three way diff on the
 * remote commit, last local commit and the base. It is also used for the 2 way
 * diff with the Last Local Commit
 * 
 */
public class GitRevisionURLHandler extends URLStreamHandler {

	/**
	 * The git protocol
	 */
	public static final String GIT_PROTOCOL = "git";

	/**
	 * Connection class for XML files in archives.
	 */
	private static class GitRevisionConnection extends URLConnection {

		/**
		 * The "file object" used to get the input stream from. The jgit library
		 * uses this kind of object(ObjectId) to point to commits.
		 */
		private ObjectId fileObject;

		/**
		 * Path obtained from the URL. The path is relative to the selected
		 * repository
		 */
		private String path;

		/**
		 * The host which is used to let the user write in the diff tool
		 */
		private String currentHost;

		/**
		 * Construct the connection
		 * 
		 * @param url
		 *          The URL
		 */
		protected GitRevisionConnection(URL url) throws IOException {
			super(url);
			setDoOutput(true);

			try {
				GitAccess gitAccess = GitAccess.getInstance();
				String host = getHost(url);
				if (GitFile.LOCAL.equals(host)) {
					fileObject = gitAccess.getLastLocalCommit();
					currentHost = GitFile.LOCAL;
				} else if (GitFile.LAST_COMMIT.equals(host)) {
					fileObject = gitAccess.getLastLocalCommit();
					currentHost = GitFile.LAST_COMMIT;
				} else if (GitFile.REMOTE.equals(host)) {
					fileObject = gitAccess.getRemoteCommit();
					currentHost = GitFile.REMOTE;
				} else if (GitFile.BASE.equals(host)) {
					fileObject = gitAccess.getBaseCommit();
					currentHost = GitFile.BASE;
				} else {
					throw new Exception("Bad syntax: " + path);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		/**
		 * Retrieves the host from the given URL
		 * 
		 * @param url
		 *          - URL to get the host
		 * @return the URL host
		 */
		private String getHost(URL url) {
			path = url.getPath();
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			String host = url.getHost();

			return host;
		}

		/**
		 * Returns an input stream that reads from this open connection.
		 * 
		 * @return the input stream
		 */
		public InputStream getInputStream() throws IOException {
			GitAccess gitAccess = GitAccess.getInstance();
			return gitAccess.getInputStream(fileObject, path);
		}

		/**
		 * Returns an output stream that writes to this connection.
		 * 
		 * @return the output stream
		 */
		public OutputStream getOutputStream() throws IOException {
			if (GitFile.LOCAL.equals(currentHost)) {
				URL fileContent = FileHelper.getFileURL(path);
				return fileContent.openConnection().getOutputStream();
			}
			throw new IOException("Writing is permitted only in the local file.");
		}

		public void connect() throws IOException {
		}

		/**
		 * @see java.net.URLConnection#getContentLength()
		 */
		@Override
		public int getContentLength() {
			return -1;
		}

		/**
		 * @see java.net.URLConnection#getContentType()
		 */
		@Override
		public String getContentType() {
			// Let Oxygen decide.
			return null;
		}
	}

	/**
	 * Creates and opens the connection
	 * 
	 * @param u
	 *          The URL
	 * @return The connection
	 */
	protected URLConnection openConnection(URL u) throws IOException {
		URLConnection connection = new GitRevisionConnection(u);
		return connection;
	}

	/**
	 * Constructs an URL for the diff tool
	 * 
	 * @param gitFile
	 *          - what file is used (Local, Last Commit, Base, Remote)
	 * @param fileLocation
	 *          - the file location relative to the repository
	 * @return the URL of the form git://gitFile/fileLocation
	 * @throws MalformedURLException
	 */
	public static URL buildURL(String gitFile, String fileLocation) throws MalformedURLException {
		URL url = null;
		if (gitFile.equals(GitFile.LOCAL)) {
			url = new URL("git://Local/" + fileLocation);
		} else if (gitFile.equals(GitFile.REMOTE)) {
			url = new URL("git://Remote/" + fileLocation);
		} else if (gitFile.equals(GitFile.BASE)) {
			url = new URL("git://Base/" + fileLocation);
		} else if (gitFile.equals(GitFile.LAST_COMMIT)) {
			url = new URL("git://LastCommit/" + fileLocation);
		} else {
			url = new URL("");
		}

		return url;
	}

}
