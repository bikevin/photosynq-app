package com.photosynq.app.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.photosynq.app.model.Option;
import com.photosynq.app.model.Question;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.CommonUtils;

public class DatabaseHelper extends SQLiteOpenHelper {
	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "PhotoSynqDB";
	// Table Names
	private static final String TABLE_RESEARCH_PROJECT = "research_project";

	// Reasearch Project Table - column names
	public static final String C_RECORD_HASH = "record_hash";
	public static final String C_ID = "id";
	public static final String C_NAME = "name";
	public static final String C_DESC = "desc";
	public static final String C_DIR_TO_COLLAB = "dir_to_collab";
	public static final String C_START_DATE = "start_date";
	public static final String C_END_DATE = "end_date";
	public static final String C_IMAGE_CONTENT_TYPE = "image_content_type";
	public static final String C_BETA = "beta";

	// Reaserch Project table create statement
	private static final String CREATE_TABLE_RESEARCH_PROJECT = "CREATE TABLE "
			+ TABLE_RESEARCH_PROJECT + "(" + C_RECORD_HASH
			+ " TEXT PRIMARY KEY," + C_ID + " TEXT," + C_NAME + " TEXT,"
			+ C_DESC + " TEXT," + C_DIR_TO_COLLAB + " TEXT," + C_START_DATE
			+ " TEXT," + C_END_DATE + " TEXT," + C_BETA + " TEXT,"
			+ C_IMAGE_CONTENT_TYPE + " TEXT" + ")";

	
	// Table Names
	private static final String TABLE_QUESTION = "question";
	private static final String TABLE_OPTION = "option";

	// Question and Option Table - column names
	public static final String C_PROJECT_HASH = "project_hash";//project_id
	public static final String C_QUESTION_TEXT = "que";//Question
	public static final String C_OPTION_TEXT = "option";//Option
	public static final String C_QUESTION_ID = "question_id";

	// Question table create statement
	private static final String CREATE_TABLE_QUESTION = "CREATE TABLE "
			+ TABLE_QUESTION + "(" + C_PROJECT_HASH
			+ " TEXT," + C_QUESTION_TEXT + " TEXT )";
	
	// Answer table create statement
	private static final String CREATE_TABLE_OPTION = "CREATE TABLE "
			+ TABLE_OPTION + "(" + C_OPTION_TEXT + " TEXT ," + C_QUESTION_ID + " TEXT)";
	
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// creating required tables
		db.execSQL(CREATE_TABLE_RESEARCH_PROJECT);
		db.execSQL(CREATE_TABLE_QUESTION);
		System.out.println(CREATE_TABLE_QUESTION);
		db.execSQL(CREATE_TABLE_OPTION);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
		// on upgrade drop older tables
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESEARCH_PROJECT);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTION);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OPTION);
		// create new tables;
		onCreate(db);

	}

	// Insert row in db
	public boolean createResearchProject(ResearchProject rp) {
		try
		{
			SQLiteDatabase db = this.getWritableDatabase();
											
			ContentValues values = new ContentValues();
			values.put(C_ID, null != rp.getId() ? rp.getId() : "");
			values.put(C_NAME, null != rp.getName() ? rp.getName() : "");
			values.put(C_DESC, null != rp.getDesc() ? rp.getDesc() : "");
			values.put(C_DIR_TO_COLLAB,null != rp.getDir_to_collab() ? rp.getDir_to_collab() : "");
			values.put(C_START_DATE,null != rp.getStart_date() ? rp.getStart_date() : "");
			values.put(C_END_DATE, null != rp.getEnd_date() ? rp.getEnd_date() : "");
			values.put(C_BETA, null != rp.getBeta() ? rp.getBeta() : "");
			values.put(C_IMAGE_CONTENT_TYPE,null != rp.getImage_content_type() ? rp.getImage_content_type(): "");
			values.put(C_RECORD_HASH, CommonUtils.getRecordHash(rp));
			// insert row
			long row_id = db.insert(TABLE_RESEARCH_PROJECT, null, values);
	
			if (row_id >= 0) {
				return true;
			} else {
				return false;
			}
		}catch (SQLiteConstraintException contraintException)
		{
			// If data already present then handle the case here.
			Log.d("DATABASE_HELPER_RESEARCH_PROJECTS", "Record already present in database for record hash ="+CommonUtils.getRecordHash(rp));
			return false;
			
		}
		catch (SQLException sqliteException){
			return false;
		}

	}

	//Fetch row from db
	public ResearchProject getResearchProject(String recordHash ) {
	    SQLiteDatabase db = this.getReadableDatabase();
	 
	    String selectQuery = "SELECT  * FROM " + TABLE_RESEARCH_PROJECT + " WHERE "
	            + C_RECORD_HASH + " = '" + recordHash + "'";
	 
	    Log.e("DATABASE_HELPER_getResearchProject", selectQuery);
	 
	    Cursor c = db.rawQuery(selectQuery, null);
	 
	    if (c != null)
	        c.moveToFirst();
	 
	    ResearchProject rp = new ResearchProject();
	    rp.setId(c.getString(c.getColumnIndex(C_ID)));
	    rp.setName(c.getString(c.getColumnIndex(C_NAME)));
	    rp.setDesc(c.getString(c.getColumnIndex(C_DESC)));
	    rp.setDir_to_collab(c.getString(c.getColumnIndex(C_DIR_TO_COLLAB)));
	    rp.setStart_date(c.getString(c.getColumnIndex(C_START_DATE)));
	    rp.setEnd_date(c.getString(c.getColumnIndex(C_END_DATE)));
	    rp.setImage_content_type(c.getString(c.getColumnIndex(C_IMAGE_CONTENT_TYPE)));
	    rp.setRecord_hash(c.getString(c.getColumnIndex(C_RECORD_HASH)));
	    rp.setBeta(c.getString(c.getColumnIndex(C_BETA)));

	    return rp;
	}
	
	//Fetch row from db
		public List<ResearchProject> getAllResearchProjects() {
		    SQLiteDatabase db = this.getReadableDatabase();
		    List<ResearchProject> researchProjects = new ArrayList<ResearchProject>();
		    String selectQuery = "SELECT  * FROM " + TABLE_RESEARCH_PROJECT ;
		 
		    Log.e("DATABASE_HELPER_getAllResearchProject", selectQuery);
		 
		    Cursor c = db.rawQuery(selectQuery, null);
		 
		    if (c.moveToFirst()) {
		        do {
		        	ResearchProject rp = new ResearchProject();
				    rp.setId(c.getString(c.getColumnIndex(C_ID)));
				    rp.setName(c.getString(c.getColumnIndex(C_NAME)));
				    rp.setDesc(c.getString(c.getColumnIndex(C_DESC)));
				    rp.setDir_to_collab(c.getString(c.getColumnIndex(C_DIR_TO_COLLAB)));
				    rp.setStart_date(c.getString(c.getColumnIndex(C_START_DATE)));
				    rp.setEnd_date(c.getString(c.getColumnIndex(C_END_DATE)));
				    rp.setImage_content_type(c.getString(c.getColumnIndex(C_IMAGE_CONTENT_TYPE)));
				    rp.setRecord_hash(c.getString(c.getColumnIndex(C_RECORD_HASH)));
				    rp.setBeta(c.getString(c.getColumnIndex(C_BETA)));
		 
		            // adding to todo list
				    researchProjects.add(rp);
		        } while (c.moveToNext());
		    }

		    return researchProjects;
		}
		
	/*
	 * Updating a Research Project
	 */
	public boolean updateResearchProject(ResearchProject rp) {
		
		//recordhash is unique record identifier if its not present then return false 
		
		if(null==rp.getRecord_hash() )
		{
			return false;
		}
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(C_ID, null != rp.getId() ? rp.getId() : "");
		values.put(C_NAME, null != rp.getName() ? rp.getName() : "");
		values.put(C_DESC, null != rp.getDesc() ? rp.getDesc() : "");
		values.put(C_DIR_TO_COLLAB,null != rp.getDir_to_collab() ? rp.getDir_to_collab() : "");
		values.put(C_START_DATE,null != rp.getStart_date() ? rp.getStart_date() : "");
		values.put(C_END_DATE, null != rp.getEnd_date() ? rp.getEnd_date() : "");
		values.put(C_BETA, null != rp.getBeta() ? rp.getBeta() : "");
		values.put(C_IMAGE_CONTENT_TYPE,null != rp.getImage_content_type() ? rp.getImage_content_type(): "");
		values.put(C_RECORD_HASH, rp.getRecord_hash());

		
		int rowsaffected = db.update(TABLE_RESEARCH_PROJECT, values, C_RECORD_HASH + " = ?",
							new String[] { String.valueOf(rp.getRecord_hash()) });
		// if update fails that indicates there is no then create new row
		if(rowsaffected <= 0)
		{
			return createResearchProject(rp);
		}
		// updating row
		return false;
	}
	
	/*
	 * Deleting a todo
	 */
	public void deleteResearchProject(String recordHash) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    db.delete(TABLE_RESEARCH_PROJECT, C_RECORD_HASH + " = ?",
	            new String[] { String.valueOf(recordHash) });
	}
	
	public boolean createQuestion(Question que) {
		try
		{
			SQLiteDatabase db = this.getWritableDatabase();
											
			ContentValues values = new ContentValues();
			values.put(C_QUESTION_TEXT, null != que.getQuestion_text() ? que.getQuestion_text() : "");
			values.put(C_PROJECT_HASH, que.getProject_hash());
			// insert row
			long row_id = db.insert(TABLE_QUESTION, null, values);
			System.out.println("This is row ID" + row_id);
			if (row_id >= 0) {
				return true;
			} else {
				return false;
			}
		}catch (SQLiteConstraintException contraintException)
		{
			// If data already present then handle the case here.
			return false;
		}
		catch (SQLException sqliteException){
			return false;
		}
	}
	
	//Insert option row in db.
	public boolean createOption(Option op) {
		try
		{
			SQLiteDatabase db = this.getWritableDatabase();
											
			ContentValues values = new ContentValues();
			values.put(C_OPTION_TEXT, null != op.getOption_text() ? op.getOption_text() : "");
			values.put(C_QUESTION_ID,op.getQuestion_id());
		//	values.put(C_PROJECT_HASH, CommonUtils.getRecordHash(que));
			// insert row
			long row_id = db.insert(TABLE_OPTION, null, values);
			if (row_id >= 0) {
				return true;
			} else {
				return false;
			}
		}catch (SQLiteConstraintException contraintException)
		{
			// If data already present then handle the case here.
			return false;
		}
		catch (SQLException sqliteException){
			return false;
		}
	}
	
	public List<Question> getAllQuestionForProject(String project_hash) {
	    SQLiteDatabase db = this.getReadableDatabase();
	    List<Question> questions = new ArrayList<Question>();
	    String selectQuery = "SELECT  * FROM " + TABLE_QUESTION + " WHERE " + C_PROJECT_HASH + " = " + project_hash;
	    System.out.println(selectQuery);
	    Log.e("DATABASE_HELPER_getAllQuestion", selectQuery);
	 
	    Cursor c = db.rawQuery(selectQuery, null);
	 
	    if (c.moveToFirst()) {
	        do {
	        	Question que = new Question();
			    que.setQuestion_text(c.getString(c.getColumnIndex(C_QUESTION_TEXT)));
			    que.setProject_hash(c.getString(c.getColumnIndex(C_PROJECT_HASH)));
			    que.setQuestion_id(c.getString(c.getColumnIndex(C_ID)));
			    //que.setRecord_hash(c.getString(c.getColumnIndex(C_RECORD_HASH)));			 
	            
			    // adding to todo list
			    questions.add(que);
	        } while (c.moveToNext());
	    }
	    return questions;
	}
	
	//Fetch row from Option db
	public List<Option> getAllOptionsForQuestion(String question_id) {
	    SQLiteDatabase db = this.getReadableDatabase();
	    List<Option> option = new ArrayList<Option>();
	    String selectQuery = "SELECT  * FROM " + TABLE_OPTION + " WHERE " + C_ID + " = " + question_id;
	    System.out.println(selectQuery);
	    Log.e("DATABASE_HELPER_getAllOption", selectQuery);
	 
	    Cursor c = db.rawQuery(selectQuery, null);
	 
	    if (c.moveToFirst()) {
	        do {
	        	Option op = new Option();
			    op.setOption_id(c.getString(c.getColumnIndex(C_ID)));
			    op.setQuestion_id(c.getString(c.getColumnIndex(C_QUESTION_ID)));
			    op.setOption_text(c.getString(c.getColumnIndex(C_OPTION_TEXT)));
			    //que.setRecord_hash(c.getString(c.getColumnIndex(C_RECORD_HASH)));			 
			    
			    // adding to todo list
			    option.add(op);
	        } while (c.moveToNext());
	    }

	    return option;
	}
	
	
	// closing database
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }
    
	
}
