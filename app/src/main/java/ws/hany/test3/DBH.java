package ws.hany.test3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBH extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Graphs.db";
    private static final int DATABASE_VERSION = 2;

    public static final String HANY_TABLE_NAME = "graphs";
    public static final String HANY_COLUMN_ID = "_id";
    public static final String HANY_COLUMN_VALUE = "value";
    public static final String HANY_COLUMN_BPM = "BPM";
    public static final String HANY_COLUMN_DATE = "date";

    public DBH(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + HANY_TABLE_NAME +
                        "(" + HANY_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                        HANY_COLUMN_VALUE + " TEXT, " +
                        HANY_COLUMN_BPM + " TEXT, " +
                        HANY_COLUMN_DATE + " TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + HANY_TABLE_NAME);
        onCreate(db);
    }

    public void insertECG(String value, String BPM, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(HANY_COLUMN_VALUE, value);
        contentValues.put(HANY_COLUMN_BPM, BPM);
        contentValues.put(HANY_COLUMN_DATE, date);

        db.insert(HANY_TABLE_NAME, null, contentValues);
    }

    public Cursor getAllECGs() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery( "SELECT * FROM " + HANY_TABLE_NAME, null );
    }

}