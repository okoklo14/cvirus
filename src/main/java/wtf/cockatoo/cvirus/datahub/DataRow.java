package wtf.cockatoo.cvirus.datahub;

import java.util.Arrays;
import java.util.List;

public class DataRow {

    public DataRow(final String colDate, final String colCountry, final int colConfirmed, final int colRecovered, final int colDeaths) {
        this.colDate = colDate;
        this.colCountry = colCountry;
        this.colConfirmed = colConfirmed;
        this.colRecovered = colRecovered;
        this.colDeaths = colDeaths;
    }


    public List<Object> toArrayList() {
        return Arrays.asList(colDate, colCountry, colConfirmed, colRecovered, colDeaths, colNew, colTodayConfirm, colTodayDeath);
    }

    String colDate;
    String colCountry;
    int colConfirmed;
    int colRecovered;
    int colDeaths;
    int colNew;
    int colTodayConfirm;
    int colTodayDeath;
}

