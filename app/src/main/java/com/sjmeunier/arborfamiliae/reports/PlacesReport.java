package com.sjmeunier.arborfamiliae.reports;

import android.content.Context;

import com.sjmeunier.arborfamiliae.AncestryUtil;
import com.sjmeunier.arborfamiliae.data.NameFormat;
import com.sjmeunier.arborfamiliae.database.AppDatabase;
import com.sjmeunier.arborfamiliae.database.Family;
import com.sjmeunier.arborfamiliae.database.Individual;
import com.sjmeunier.arborfamiliae.database.Place;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlacesReport extends BaseReport {

    private Map<String, Integer> placesWithCount;
    private List<String> places;
    private List<Integer> ancestorIds;

    public PlacesReport(Context context, AppDatabase database, Map<Integer, Place> placesInActiveTree, Map<Integer, Individual> individualsInActiveTree, Map<Integer, Family> familiesInActiveTree, NameFormat nameFormat, int maxGenerations, int treeId) {
        super(context, database, placesInActiveTree, individualsInActiveTree, familiesInActiveTree, nameFormat, maxGenerations, treeId);
    }

    @Override
    public boolean generateReport(String filename, int activeIndividualId) throws IOException {
        this.configureOutputFile(filename);
        ancestorIds = new ArrayList<>();
        placesWithCount = new HashMap<>();
        places = new ArrayList<>();

        if (this.individualsInActiveTree.containsKey(activeIndividualId)) {
            Individual individual = this.individualsInActiveTree.get(activeIndividualId);
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
            this.writeLine("Places report of " + AncestryUtil.generateName(individual, this.nameFormat));
            this.writeLine("Generated by Arbor Familiae at " + timeStamp);
            this.writeLine("");
            this.writeLine("");
        }
        this.processIndividual(activeIndividualId, 1, 1);
        this.outputReport();
        this.closeFile();

        return true;
    }

    private void outputReport() throws IOException {
        Collections.sort(places);
        for(String place : places) {
            this.writeLine(place + "(" + String.valueOf(placesWithCount.get(place)) + ")");
        }
    }

    private void processIndividual(int individualId, int generation, long ahnenNumber) {
        if (!this.individualsInActiveTree.containsKey(individualId))
            return;

        if (!ancestorIds.contains(individualId) && this.individualsInActiveTree.containsKey(individualId)) {
            ancestorIds.add(individualId);
            Individual individual = this.individualsInActiveTree.get(individualId);

            if (individual.birthPlace != -1 && this.placesInActiveTree.get(individual.birthPlace) != null) {
                Place place = this.placesInActiveTree.get(individual.birthPlace);
                if (placesWithCount.containsKey(place.placeName)) {
                    placesWithCount.put(place.placeName, placesWithCount.get(place.placeName) + 1);
                } else {
                    placesWithCount.put(place.placeName, 1);
                    places.add(place.placeName);
                }
            }
            if (individual.burialPlace != -1 && this.placesInActiveTree.get(individual.burialPlace) != null) {
                Place place = this.placesInActiveTree.get(individual.burialPlace);
                if (placesWithCount.containsKey(place.placeName)) {
                    placesWithCount.put(place.placeName, placesWithCount.get(place.placeName) + 1);
                } else {
                    placesWithCount.put(place.placeName, 1);
                    places.add(place.placeName);
                }
            }

            if (individual.diedPlace != -1 && this.placesInActiveTree.get(individual.diedPlace) != null) {
                Place place = this.placesInActiveTree.get(individual.diedPlace);
                if (placesWithCount.containsKey(place.placeName)) {
                    placesWithCount.put(place.placeName, placesWithCount.get(place.placeName) + 1);
                } else {
                    placesWithCount.put(place.placeName, 1);
                    places.add(place.placeName);
                }
            }

            if (individual.burialPlace != -1 && this.placesInActiveTree.get(individual.burialPlace) != null) {
                Place place = this.placesInActiveTree.get(individual.burialPlace);
                if (placesWithCount.containsKey(place.placeName)) {
                    placesWithCount.put(place.placeName, placesWithCount.get(place.placeName) + 1);
                } else {
                    placesWithCount.put(place.placeName, 1);
                    places.add(place.placeName);
                }
            }

            List<Family> families = database.familyDao().getAllFamiliesForHusbandOrWife(individual.treeId, individualId);
            for(Family family : families) {
                if (family.marriagePlace != -1 && this.placesInActiveTree.get(family.marriagePlace) != null) {
                    Place place = this.placesInActiveTree.get(family.marriagePlace);
                    if (placesWithCount.containsKey(place.placeName)) {
                        placesWithCount.put(place.placeName, placesWithCount.get(place.placeName) + 1);
                    } else {
                        placesWithCount.put(place.placeName, 1);
                        places.add(place.placeName);
                    }
                }
            }

            if (generation < this.maxGenerations) {
                if (individual.parentFamilyId != -1 && this.familiesInActiveTree.containsKey(individual.parentFamilyId)) {
                    Family family = this.familiesInActiveTree.get(individual.parentFamilyId);
                    if (family.husbandId != -1) {
                        processIndividual(family.husbandId, generation + 1, ahnenNumber * 2);
                    }
                    if (family.wifeId != -1) {
                        processIndividual(family.wifeId, generation + 1, ahnenNumber * 2 + 1);
                    }
                }
            }
        } else {
            return;
        }

    }
}
