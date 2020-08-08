package com.belamila.backend.parser;

import com.belamila.model.Package;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by: Bartosz Nawrot
 * Date: 19.05.2020
 * Description:
 */
public class CsvReader {

    private static final Logger logger = LoggerFactory.getLogger(CsvReader.class);

    private HashMap<String, List<String>> loadCsv(File file) throws RuntimeException, IOException, CsvValidationException {
        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(file))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                records.add(Arrays.asList(values));
            }
        }
        HashMap<String, List<String>> result = new HashMap<>();
        records.forEach(list -> result.put(list.get(0), list));
        return result;
    }

    public List<Package> read(File file) throws Exception {
        Map<String, List<String>> csv = loadCsv(file);
        ArrayList<Package> packages = new ArrayList<>();
        csv.forEach((key, val) -> {
            String service = parseService(val.get(20));
            if (service != null) {
                Package pack = Package.builder()
                        .service(service)
                        .receiver(val.get(10))
                        .address(val.get(15))
                        .city(val.get(14))
                        .email(val.get(19))
                        .zip(val.get(16).replace("\"", ""))
                        .phone(val.get(17).replace("\"", ""))
                        .build();
                packages.add(pack);
                logger.debug("{}", pack);
            }
        });
        return packages;
    }

    private String parseService(String deliveryMethod) throws RuntimeException {
        if (deliveryMethod.equals("Kurier DPD ") || deliveryMethod.equals("DPD In Advance ")) {
            return "DPD Classic";
        } else if (deliveryMethod.equals("Paczkomaty Inpost Proszę o maila z danymi paczkomatu")) {
            return "INPOST";
        } else {
            return null;
        }
    }
}
