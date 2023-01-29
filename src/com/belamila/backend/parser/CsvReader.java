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
import java.util.stream.IntStream;

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
            logger.info("Parsing: {}", val);
            //printFieldsWithIds(val);
            String service = parseService(val.get(7));
            if (service != null) {
                Package pack = Package.builder()
                        .id(val.get(0))
                        .service(service)
                        .receiver(val.get(9))
                        .address(val.get(15))
                        .city(val.get(14))
                        .email(val.get(4))
                        .zip(val.get(16).replace("\"", ""))
                        .phone(val.get(10).replace("\"", ""))
                        .amount(parseAmount(val.get(7), val.get(32)))
                        .build();
                packages.add(pack);
                logger.debug("Result: {}", pack);
            }
        });
        return packages;
    }

    private String parseService(String deliveryMethod) throws RuntimeException {
        if (deliveryMethod.contains("Kurier DPD")
                || deliveryMethod.contains("DPD In Advance")
                || deliveryMethod.contains("DPD Pobranie")) {
            return "DPD Classic";
        } else if (deliveryMethod.contains("Paczkomaty InPost")
                || deliveryMethod.contains("Paczkomat InPost")
                || deliveryMethod.contains("Inpost")) {
            return "INPOST";
        } else {
            logger.warn("Unrecognized delivery method: {}", deliveryMethod);
            return null;
        }
    }

    private Double parseAmount(String deliveryMethod, String value) {
        double result = -1.0;
        if (deliveryMethod.contains("DPD Pobranie")) {
            try {
                result = Double.parseDouble(value);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private void printFieldsWithIds(List<String> val) {
        IntStream.range(0, val.size())
                .forEach(i -> logger.info("Filed: {}: {}", i, val.get(i)));
    }
}
