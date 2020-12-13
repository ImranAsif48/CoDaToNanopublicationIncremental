/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codatonanopublicationincremental;

import java.io.IOException;
import org.nanopub.MalformedNanopubException;

/**
 *
 * @author IMRAN ASIF
 */
public class CoDaToNanopublicationIncremental {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, MalformedNanopubException {
        //LoadNanopubDataset load = new LoadNanopubDataset();
        //load.SplitNanoIntoSeparateFiles("nanopubs_r_more_prov_20_08_2020.trig");
        NanopubFromCoDa codaToNp = new NanopubFromCoDa();
        codaToNp.GenerateNanopubsForMeasure("d", "https://api.cooperationdatabank.org/datasets/coda-dev/databank/services/databank/sparql");
    }
    
}
