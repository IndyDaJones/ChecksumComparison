import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * 
 * @author j.nyffeler
 *
 */
public class Generate {
	private static final Logger log = Logger.getLogger( ChecksumGenerator.class.getName() );
	DBHandler db;
	String checksum;
	FileHandler file;
	public Generate(String Mchecksum){
		file = new FileHandler();
		checksum = Mchecksum;
	}
	/**
	 * The method starts the calcualtion
	 */
	public void start() {
		log.log(Level.INFO,"create DBHandler");
		db = new DBHandler();
		log.log(Level.INFO,"DBHandler created");
		log.log(Level.INFO,"Startup Checksum calcualtion");
		calculateChecksum();
	}
	
	/**
	 * In this method the actual checksum for the source file is calculated
	 */
	private void calculateChecksum() {
		log.log(Level.INFO,"Get all FilePaths");
		ResultSet folders = db.GetFolderOfFiles();
		log.log(Level.INFO,"FilePaths gathered!");
		try {
			while((folders != null) && (folders.next())) {
				if(folders.getString(1) != null && folders.getString(2) != null) { // ID // FOLDER
					ArrayList<String> files = getLatestSequencesFromFolder(folders.getString(2));
					for ( int i = 0; i < files.size(); i++) {
						String path = preparePath(folders.getString(2), files.get(i), "");
						log.log(Level.INFO,"FilePath is " + path);
						calculateChecksum(path, folders.getString(2), files.get(i));	
					}
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE,e.getLocalizedMessage());
			folders = null;
		}
	}
	
	/**
	 * This sequence completes the sequence number with leading 0s.
	 * @param SequenceNumber
	 * @return Sequence number with corresponding leading 0s.
	 */
	private String completeSequenceNumber(String SequenceNumber) {
		String sequence = "";
		switch (SequenceNumber.length()) {
        case 1:  sequence = "000"+ SequenceNumber;
                 break;
        case 2:  sequence = "00"+ SequenceNumber;
                 break;
        case 3:  sequence = "0"+ SequenceNumber;
                 break;
        case 4:  sequence = SequenceNumber;
                 break;
        default: sequence = "Invalid sequence number";
                 break;
		}
		return sequence;
	}
	
	/**
	 * In this method the checksum is being calculated and stored in the database
	 * @param path Path to the file containing the sourcode from which a checksum is needed
	 * @param ID Identifier of the database record where the checksum is stored after calculation
	 */
	private void calculateChecksum(String path, String ID) {
		try {
			File Source = new File(path);
			log.log(Level.INFO,"Source loaded " + path);
			String file_checksum = FileHandler.getFileChecksum(checksum, Source);
			log.log(Level.INFO,"Checksum calculated " + file_checksum + "for sequence with ID "+ ID);
			db.updatePath(ID, file_checksum);
			// Let Thread sleep for a while so that the DB can do it's job properly! (Access DB)
			Thread.sleep(file.getDBTimeout());
		}catch (IOException e) {
			db.updatePath(ID, e.getLocalizedMessage());
			log.log(Level.SEVERE,e.getLocalizedMessage());
		}catch (Exception e) {
			db.updatePath(ID, e.getLocalizedMessage());
			log.log(Level.SEVERE,e.getLocalizedMessage());
		}
	}
	
	/**
	 * In this method the checksum is being calculated and stored in the database
	 * @param path Path to the file containing the sourcode from which a checksum is needed
	 * @param ID Identifier of the database record where the checksum is stored after calculation
	 */
	private void calculateChecksum(String path, String System, String Sequence) {
		try {
			File Source = new File(path);
			log.log(Level.INFO,"Source loaded " + path);
			String file_checksum = FileHandler.getFileChecksum(checksum, Source);
			log.log(Level.INFO,"Checksum calculated " + file_checksum + "for sequence with path "+ path);
			db.insertData(System, System, Sequence ,checksum);
			// Let Thread sleep for a while so that the DB can do it's job properly! (Access DB)
			Thread.sleep(file.getDBTimeout());
		}catch (IOException e) {
			db.insertData(System, System, Sequence ,e.getLocalizedMessage());
			log.log(Level.SEVERE,e.getLocalizedMessage());
		}catch (Exception e) {
			db.insertData(System, System, Sequence ,e.getLocalizedMessage());
			log.log(Level.SEVERE,e.getLocalizedMessage());
		}
	}
	
	/**
	 * This method generates the file path to the source file located on the local machine. (Usually a SVN- Pepository)
	 * @param System System where the sequence is active
	 * @param SequnceNumber sequence number
	 * @param SequenceName sequence name
	 * @return Absolute path the source file of the sequence 
	 */
	private String preparePath(String System, String SequnceNumber, String SequenceName) {
		String path = preparePathATKPlus(System, SequnceNumber, SequenceName);
		if (!isValidFile(path)) {
			path = preparePathATK32(System, SequnceNumber);
		}
		return path;
	}
	private String preparePathATKPlus(String System, String SequnceNumber, String SequenceName) {
		String path = "";
		//FileHandler file = new FileHandler();
		if(System != null && SequnceNumber != null) {
			//It is a sequence
			path = file.getSourceFolderLocation() +"/"+ System+"/F10_Sources/"+completeSequenceNumber(SequnceNumber)+"_"+SequenceName+".JSEQ";
		}else if(System != null && SequnceNumber == null) {
			//It is a global sequence
			path = file.getGlobalSourceFolderLocation()+"/"+SequenceName+".JSEQ";
		}
		else {
			//It is a include
			path = file.getIncludesFolderLocation() +"/"+ SequenceName+".Jinc";
		}
		path = path.replace("\\","/");
		while (path.indexOf("/") != -1) {
    		String folder = path.substring(0, path.indexOf("/"));
    		
    		if (folder.equals(file.getSourceFolderLocation())||folder.equals(file.getIncludesFolderLocation())||folder.equals(file.getGlobalSourceFolderLocation())) {
    			log.log(Level.INFO,"FolderPath is " + folder);
    			break;
    		}
    		path = path.substring(path.indexOf("/")+1, path.length());
    		log.log(Level.INFO,"FilePath is " + path);
		} 
		path = "/"+path;
		path = file.getFileLocation()+path;
		log.log(Level.INFO,"FilePath is " + path);
		path = path.replace("/", "\\");
		
		return path;
	}
	/**
	 * Prepares the path to the sequence for the checksum calcualtion
	 * @param System
	 * @param SequnceNumber
	 * @param SequenceName
	 * @return
	 */
	private String preparePathATK32(String System, String SequenceNumber) {
		String path = "";
		//FileHandler file = new FileHandler();
		if(System != null && SequenceNumber != null) {
			//It is a sequence
			path = file.getFileLocation() +"/"+ System+"/"+getLatestSequenceVersion(System, SequenceNumber);
		}
		path = path.replace("\\","/");
		
		log.log(Level.INFO,"FilePath is " + path);
		path = path.replace("/", "\\");
		
		return path;
	}
	/**
	 * Gives the latest version number from a set of sequences 
	 * @param System
	 * @param SequenceNumber
	 * @return
	 */
	private String getLatestSequenceVersion(String System, String SequenceNumber) {
		String path = file.getFileLocation() +"/"+ System+"/";
		File rep = new File(path);
		File[] list = rep.listFiles();
		rep = null;
		ArrayList<String> filenames = new ArrayList<String>();
		for ( int i = 0; i < list.length; i++) {
			if (list[i].getName().contains("SEQ"+SequenceNumber+"_X") && list[i].getName().contains(".SEQ"))
		    filenames.add(list[i].getName());
		}
		Collections.sort(filenames.subList(0, filenames.size()));
		String version = filenames.get(filenames.size()-1);
		return version;
	}
	
	/**
	 * Gives the latest version number from a set of sequences 
	 * @param System
	 * @param SequenceNumber
	 * @return
	 */
	private ArrayList<String> getLatestSequencesFromFolder(String System) {
		String path = file.getFileLocation() +"/"+ System+"/";
		File rep = new File(path);
		File[] list = rep.listFiles();
		rep = null;
		ArrayList<String> filenames = new ArrayList<String>();
		for ( int i = 0; i < list.length; i++) {
			if (list[i].getName().contains("SEQ") && list[i].getName().contains(".SEQ"))
		    filenames.add(list[i].getName());
		}
		Collections.sort(filenames.subList(0, filenames.size()));
		String version = filenames.get(filenames.size()-1);
		
		ArrayList<String> result = new ArrayList<String>();
		for ( int i = 0; i < filenames.size(); i++) {
			String sequenceNumb = getSequenceNumberFromFileName(filenames.get(i));
			String latestVersion = getLatestSequenceVersion(System, sequenceNumb);
			log.log(Level.INFO,"FilePath is " + latestVersion);
			if(!result.contains(latestVersion)) {
				result.add(latestVersion);
			}
		}
		return result;
	}
	private String getSequenceNumberFromFileName(String SequenceFileName) {
		// SEQ0023_X001Y000.SEQ /[0-9][0-9][0-9][0-9]
		String result = SequenceFileName.substring(SequenceFileName.indexOf("Q")+1, SequenceFileName.indexOf("X")-1);
		return result;
	}
	private boolean isValidFile(String path) {
		try {
			log.log(Level.INFO,"Try to load source  with path " + path);
			File Source = new File(path);
			log.log(Level.INFO,"Source successfully loaded " + path);
			//Get file input stream for reading the file content
			//FileInputStream fis = new FileInputStream(Source);
			//close the stream; We don't need it now.
			//fis.close();
			return Source.canRead();
		}catch (Exception e) {
			log.log(Level.SEVERE,e.getLocalizedMessage());
			return false;
		}
	}
}
