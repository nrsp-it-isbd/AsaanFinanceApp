package com.assan.finance.sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqliteDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "db";
    private static final int DB_VERSION = 7;
    private static final String CREATE_CENSUS_TABLE = "CREATE TABLE IF NOT EXISTS CENSUS(ID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "DISTRICT_ID TEXT, DISTRICT TEXT, TEHSIL_ID TEXT, TEHSIL TEXT, UC_ID TEXT, UNIONCOUNCIL TEXT, REVENUEVILLAGE_ID TEXT," +
            " REVENUEVILLAGE TEXT)";
    private static final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS USERS(ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " USER_ID TEXT, USER_PASSWORD TEXT, USER_STATUS TEXT DEFAULT 0)";
    private static final String CREATE_FARMER_APPLICATION_TABLE = "CREATE TABLE IF NOT EXISTS FARMER_APPLICATION" +
            " (ID INTEGER PRIMARY KEY AUTOINCREMENT,DISTRICT_ID TEXT, TEHSIL_ID TEXT, UNIONCOUNCIL_ID TEXT," +
            " REVENUE_VILLAGE_ID TEXT, LATITUDE TEXT, LONGITUDE TEXT, FARMER_NAME TEXT, FARMER_PARENTAGE TEXT, FARMER_CNIC TEXT, FARMER_PHONE_NO TEXT," +
            " FARMER_GENDER TEXT, FARMER_PHONE_TYPE TEXT, FARMER_INCOME_CULTIVATION TEXT, FARMER_INCOME_EMPLOYMENT TEXT," +
            " FARMER_INCOME_BUSINESS TEXT, FARMER_INCOME_LIVESTOCK TEXT, FARMER_INCOME_FISHFARMING TEXT," +
            " FARMER_MONTHLY_INCOME TEXT, FARMER_YEARLY_INCOME TEXT, " +

            //SECTION(2) CULTIVABLE-LAND_SECTION

            " CULTIVABLE_LAND_SELF TEXT, FARM_SOIL_TESTED_SELF TEXT, FARM_WATER_TESTED_SELF TEXT," +
            " CULTIVABLE_LAND_THAIKA TEXT, FARM_SOIL_TESTED_THAIKA TEXT, FARM_WATER_TESTED_THAIKA TEXT," +
            " CULTIVABLE_LAND_KANALS TEXT, CULTIVABLE_LAND_ADDRESS TEXT, LAND_NEAREST_MANDI TEXT," +
            " DISTANCE_FROM_NRSP_APCL TEXT, UREA TEXT, DAP TEXT, SINGLE_SUPER_POTASH TEXT, POTASH TEXT, ZINC TEXT, " +
            " IRRIGATION_SOURCE_CANAL  TEXT, IRRIGATION_SOURCE_TUBEWELL  TEXT, TUBEWELL_ELECTRICITY  TEXT, TUBEWELL_SOLAR TEXT, TUBEWELL_DIESEL TEXT, FERTILIZERS_USED TEXT, " +
            " FERTILIZERS_ID TEXT, PESTICIDES_USED TEXT, PESTICIDES_ID TEXT, SEEDS_USED TEXT, SEEDS_ID TEXT, " +

            //SECTION(3)  FARM-INPUTS

            " SEEDS_USAGE TEXT, SEEDS_SOW_PER_ACRE TEXT, PLANTS_SOW_PER TEXT, MONTH_OF_TRANSPLANTATION TEXT, HOW_IS_TRANSPLANTATION_DONE TEXT," +
            " CROP_RABI TEXT, PESTICIDES_USED_RABI TEXT, PESTICIDES_QUANTITY_RABI TEXT, PESTICIDES_MONTH_RABI TEXT, WEEDICIDES_USED_RABI TEXT," +
            " WEEDICIDES_QUANTITY_RABI TEXT, WEEDICIDES_MONTH_RABI TEXT, GRANURAL_TYPE_RABI TEXT, GRANURAL_QUANTITY_RABI TEXT, GRANURAL_MONTH_RABI TEXT," +
            " RABI_PRODUCTION TEXT, CROP_STORAGE_PROCEDURE_RABI TEXT, CROP_KHAREEF TEXT, PESTICIDES_USED_KHAREEF TEXT, PESTICIDES_QUANTITY_KHAREEF TEXT," +
            " PESTICIDES_MONTH_KHAREEF TEXT, WEEDICIDES_USED_KHAREEF TEXT, WEEDICIDES_QUANTITY_KHAREEF TEXT, WEEDICIDES_MONTH_KHAREEF TEXT, GRANURAL_TYPE_KHAREEF TEXT," +
            " GRANURAL_QUANTITY_KHAREEF TEXT, GRANURAL_MONTH_KHAREEF TEXT, KHAREEF_PRODUCTION TEXT, CROP_STORAGE_PROCEDURE_KHAREEF TEXT, " +

            //SECTION(4)  POST HARVEST PRACTICES

            " HOW_IS_CROP_HARVESTED TEXT, LABOUR_PAID TEXT, HARVESTOR_PAID TEXT, TRACTOR TEXT, HARVESTOR TEXT, ROTAVATOR TEXT, SEED_DRILL TEXT, " +

            //SECTION(5)  PARTNERSHIP WITH NRSP-APCL

            " PARTNERSHIP_WITH_NRSP_APCL TEXT, STORE_UPCOMMING_RICE_WITH_NRSP_APCL TEXT, SALE_RICE_CROP_TO_NRSP_APCL TEXT, REQUIRED_ADVISORY TEXT, REQUIRED_ASSISTANCE TEXT," +
            " REQUIRED_LAND_TESTING TEXT, REQUIRED_WATER_TESTING TEXT, REQUIRED_FERTILIZER TEXT, REQUIRED_SEEDS TEXT, REQUIRED_MACHINERY_ON_RENT TEXT," +
            " REQUIRED_CLEANING_OF_CROP TEXT, REQUIRED_DRYING_OF_CROP TEXT, REQUIRED_LOAN TEXT, REQUIRED_ASSISTANCE_IN_TRANSPORTATION TEXT, REQUIRED_BARDANA TEXT," +
            " REQUIRED_CROP_WAREHOUSING TEXT, REQUIRED_CROP_PURCHASING TEXT, " +

            // LAST-SECTION-IMAGES

            " FARMERS_INSTRUCTION_IMAGE TEXT, FARMERS_IMAGE TEXT, STATUS_SENT TEXT DEFAULT 0 )";


    //SECTION(1) FARMERS-SECTION
    private static final String CREATE_SEEDS_TABLE = "CREATE TABLE IF NOT EXISTS SEEDS (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
            "SD_ID TEXT, SD_NAME TEXT, SD_TYPE TEXT)";
    private static final String CREATE_FERTILIZERS_TABLE = "CREATE TABLE IF NOT EXISTS FERTILIZERS (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
            "FR_ID TEXT, FR_NAME TEXT)";
    private static final String CREATE_PESTICIDES_TABLE = "CREATE TABLE IF NOT EXISTS PESTICIDES (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
            "PT_ID TEXT, PT_NAME TEXT)";

    private static final String CREATE_LOAN_TABLE = "CREATE TABLE LOAN_APPLICATION (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "LoanCategory TEXT," +
            "ApplicantName TEXT," +
            "ApplicantCnic TEXT," +
            "ClientCnicIssue TEXT," +
            "ClientCnicExpiry TEXT," +
            "ClientCnicDob TEXT," +
            "FatherName TEXT," +
            "FatherCnic TEXT," +
            "HusbandName TEXT," +
            "HusbandCnic TEXT," +
            "MobileNumber TEXT," +
            "PresentAddress TEXT," +
            "tehsilname TEXT," +
            "districtname TEXT," +
            "landowned TEXT," +
            "PermanentAddress TEXT," +
            "BankAccNumber TEXT," +
            "BankName TEXT," +
            "BankBranch TEXT," +
            "BankBranchCode TEXT," +
            "LoanAmount TEXT," +
            "Occupation TEXT," +
            "PurposeofLoan TEXT," +
            "DurationofLoan TEXT," +
            "BusinessAddress TEXT," +
            "HowOldBusiness TEXT," +
            "NameofFinancialInsitution TEXT," +
            "TypeofLoan TEXT," +
            "RemainingBalance," +
            "CNIC_front_IMAGE TEXT," +
            "CNIC_front_IMAGE_PATH TEXT," +
            "CNIC_back_IMAGE TEXT," +
            "CNIC_back_IMAGE_PATH TEXT," +
            "STATUS_UPLOAD TEXT DEFAULT 0)";
    private Context mContext;


    public SqliteDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        mContext = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_CENSUS_TABLE);
        db.execSQL(CREATE_FARMER_APPLICATION_TABLE);
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_SEEDS_TABLE);
        db.execSQL(CREATE_FERTILIZERS_TABLE);
        db.execSQL(CREATE_PESTICIDES_TABLE);

        db.execSQL(CREATE_LOAN_TABLE);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        db.execSQL("DROP TABLE IF EXISTS CENSUS");
        db.execSQL("DROP TABLE IF EXISTS FARMER_APPLICATION");
        db.execSQL("DROP TABLE IF EXISTS USERS");
        db.execSQL("DROP TABLE IF EXISTS SEEDS");
        db.execSQL("DROP TABLE IF EXISTS FERTILIZERS");
        db.execSQL("DROP TABLE IF EXISTS PESTICIDES");

        db.execSQL("DROP TABLE IF EXISTS LOAN_APPLICATION");


        onCreate(db);

    }


    public boolean insertdataloanapplicationfoam(ContentValues cv) {
        SQLiteDatabase db = this.getWritableDatabase();
        long rowInserted = db.insert("LOAN_APPLICATION", null, cv);
        if (rowInserted == -1) {
            return false;
        } else {
            return true;
        }
    }


    public void deleteCensus() {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM CENSUS");

    }

    public void deleteUsers() {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM USERS");

    }


    public void deleteSeeds() {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM SEEDS");

    }


    public void deleteFertilizers() {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM FERTILIZERS");

    }

    public void deletePesticides() {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM PESTICIDES");

    }

    public void addCensusSync(ContentValues cv) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("CENSUS", null, cv);

    }

    public void addUserSync(ContentValues cv) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("USERS", null, cv);

    }


    public void addSeeds(ContentValues cv) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("SEEDS", null, cv);

    }

    public void addPesticides(ContentValues cv) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("PESTICIDES", null, cv);

    }

    public void addFertilizers(ContentValues cv) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("FERTILIZERS", null, cv);

    }

    public Cursor getDistrict() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT DISTRICT_ID as _id, DISTRICT " +
                "FROM " +
                "CENSUS " +
                "GROUP BY DISTRICT_ID", null);
        return mCursor;
    }

    public Cursor getTehsil() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT DISTINCT TEHSIL_ID as _id, TEHSIL " +
                "FROM " +
                "CENSUS ORDER BY 2", null);
        return mCursor;
    }

    public Cursor getUC() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT DISTINCT UC_ID as _id, UNIONCOUNCIL " +
                "FROM " +
                "CENSUS ORDER BY 2", null);
        return mCursor;
    }

    public Cursor getRV() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT DISTINCT REVENUEVILLAGE_ID as _id, REVENUEVILLAGE " +
                "FROM " +
                "CENSUS  ORDER BY 2", null);
        return mCursor;
    }


    public Cursor spnRVFiltered(String selectedid) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c3;
        c3 = db.rawQuery("SELECT REVENUEVILLAGE_ID as _id, REVENUEVILLAGE " +
                        "FROM " +
                        "CENSUS " +
                        "WHERE UC_ID ='" + selectedid + "'" +
                        "GROUP BY REVENUEVILLAGE_ID "
                , null);


        return c3;

    }

    public Cursor getFERTILIZERS() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT  FR_ID, FR_NAME " +
                "FROM " +
                "FERTILIZERS ", null);
//        mCursor.moveToNext();
        return mCursor;
    }


    public Cursor getSEEDS() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT  SD_ID, SD_NAME " +
                "FROM " +
                "SEEDS ", null);
//        mCursor.moveToNext();
        return mCursor;
    }

    public Cursor getPESTICIDES() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT  PT_ID, PT_NAME " +
                "FROM " +
                "PESTICIDES ", null);
        //        mCursor.moveToNext();

        return mCursor;
    }


    public Cursor login(String username, String password) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT * " +
                "FROM USERS " +
                "WHERE USER_ID ='" + username + "' " +
                "AND USER_PASSWORD='" + password + "'", null);
        return mCursor;
    }


    public boolean addFarmerApplication(ContentValues cv) {

        SQLiteDatabase db = this.getWritableDatabase();

        long rowInserted = db.insert("FARMER_APPLICATION", null, cv);

        if (rowInserted == -1) {
            return false;
        } else {
            return true;
        }

    }




    // LOAN_APPLICATION

    public Cursor getLoanForSend() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT *" +
                "FROM " +
                "LOAN_APPLICATION " +
                "WHERE STATUS_UPLOAD = 0 LIMIT 1 ", null);
        return mCursor;

    }


    public boolean updateLoanSentStatus(String id) {

        ContentValues cv = new ContentValues();
        cv.put("STATUS_UPLOAD", "1");

        SQLiteDatabase db = this.getWritableDatabase();
        long rowUpdated = db.update("LOAN_APPLICATION", cv, "id=" + id, null);

        if (rowUpdated == -1) {
            return false;
        } else {
            return true;
        }

    }

    public Cursor getUnsentLoanApplications() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT FatherCnic " +
                "FROM " +
                "LOAN_APPLICATION " +
                "WHERE STATUS_UPLOAD = 0", null);
        return mCursor;

    }


    public Cursor getSentLoanApplications() {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT FatherCnic " +
                "FROM " +
                "LOAN_APPLICATION " +
                "WHERE STATUS_UPLOAD = 1", null);
        return mCursor;

    }


}
