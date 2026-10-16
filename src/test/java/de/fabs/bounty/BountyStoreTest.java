package de.fabs.bounty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class BountyStoreTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    File dir;

    private BountyStore neu(File file) {
        BountyStore store = new BountyStore(file, LOG);
        store.load();
        return store;
    }

    @Test
    void addUndSpeichern() {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        store.add(a, 10000);
        store.add(a, 2550);
        store.add(b, 500);
        assertEquals(12550, store.get(a));
        assertTrue(file.exists());

        BountyStore geladen = neu(file);
        assertEquals(12550, geladen.get(a));
        assertEquals(500, geladen.get(b));
    }

    @Test
    void auszahlungKomplett() {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID opfer = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        store.add(opfer, 700);

        assertEquals(700, store.vormerken(opfer, killer));
        assertEquals(0, store.get(opfer));
        assertEquals(1, store.offeneAuszahlungen().size());

        store.erledigt(opfer);
        BountyStore geladen = neu(file);
        assertEquals(0, geladen.get(opfer));
        assertTrue(geladen.offeneAuszahlungen().isEmpty());
    }

    @Test
    void absturzZwischenVormerkenUndAuszahlung() {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID opfer = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        store.add(opfer, 700);
        store.vormerken(opfer, killer);

        // "Absturz": neu laden, ohne erledigt() aufgerufen zu haben
        BountyStore geladen = neu(file);
        BountyStore.Auszahlung offen = geladen.offeneAuszahlungen().get(opfer);
        assertNotNull(offen);
        assertEquals(killer, offen.killer());
        assertEquals(700, offen.cent());
        assertEquals(0, geladen.get(opfer));
    }

    @Test
    void auszahlungFehlgeschlagen() {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID opfer = UUID.randomUUID();
        store.add(opfer, 700);
        store.vormerken(opfer, UUID.randomUUID());
        store.zurueck(opfer);

        BountyStore geladen = neu(file);
        assertEquals(700, geladen.get(opfer));
        assertTrue(geladen.offeneAuszahlungen().isEmpty());
    }

    // eine normale Datei als "Ordner" benutzen, dann kann darunter nichts geschrieben werden
    private File kaputterPfad() throws Exception {
        File sperre = new File(dir, "sperre");
        Files.writeString(sperre.toPath(), "x");
        return new File(sperre, "bounties.yml");
    }

    @Test
    void addBeiSpeicherfehlerAendertNichts() throws Exception {
        BountyStore store = new BountyStore(kaputterPfad(), LOG);
        UUID a = UUID.randomUUID();
        assertFalse(store.add(a, 500));
        assertEquals(0, store.get(a));
        assertTrue(store.all().isEmpty());
    }

    @Test
    void vormerkenBeiSpeicherfehlerZahltNichtAus() throws Exception {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID opfer = UUID.randomUUID();
        store.add(opfer, 700);

        // Datei durch einen Ordner gleichen Namens ersetzen, ab jetzt schlägt jedes Speichern fehl
        assertTrue(file.delete());
        assertTrue(file.mkdir());

        assertEquals(0, store.vormerken(opfer, UUID.randomUUID()));
        assertEquals(700, store.get(opfer));
        assertTrue(store.offeneAuszahlungen().isEmpty());
    }

    @Test
    void abziehenNachFehlgeschlagenerAbbuchung() {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID a = UUID.randomUUID();
        store.add(a, 1000);
        store.add(a, 500);
        assertTrue(store.abziehen(a, 500));
        assertEquals(1000, store.get(a));
        assertTrue(store.abziehen(a, 1000));
        assertEquals(0, store.get(a));
        assertTrue(neu(file).all().isEmpty());
    }

    @Test
    void abziehenBeiSpeicherfehlerAendertNichts() throws Exception {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID a = UUID.randomUUID();
        store.add(a, 1500);

        assertTrue(file.delete());
        assertTrue(file.mkdir());

        assertFalse(store.abziehen(a, 500));
        assertEquals(1500, store.get(a));
    }

    @Test
    void zurueckUndErledigtBeiSpeicherfehlerAendernNichts() throws Exception {
        File file = new File(dir, "bounties.yml");
        BountyStore store = new BountyStore(file, LOG);
        UUID opfer = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        store.add(opfer, 700);
        store.vormerken(opfer, killer);

        assertTrue(file.delete());
        assertTrue(file.mkdir());

        assertFalse(store.zurueck(opfer));
        assertEquals(0, store.get(opfer));
        assertEquals(killer, store.offeneAuszahlungen().get(opfer).killer());

        assertFalse(store.erledigt(opfer));
        assertEquals(700, store.offeneAuszahlungen().get(opfer).cent());
    }

    @Test
    void kaputteEintraegeWerdenUebersprungen() throws Exception {
        File file = new File(dir, "bounties.yml");
        Files.writeString(file.toPath(), "kopfgelder:\n  kein-uuid: 500\n  " + UUID.randomUUID() + ": -20\n");
        BountyStore store = neu(file);
        assertTrue(store.all().isEmpty());
    }

    @Test
    void allIstNurLesbar() {
        BountyStore store = new BountyStore(new File(dir, "x.yml"), LOG);
        assertThrows(UnsupportedOperationException.class, () -> store.all().put(UUID.randomUUID(), 1L));
    }
}
