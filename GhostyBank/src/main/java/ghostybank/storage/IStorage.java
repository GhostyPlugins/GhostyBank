package ghostybank.storage;

import ghostybank.data.BankData;

import java.util.Collection;
import java.util.UUID;

/**
 * Abstrakte Storage-Schnittstelle.
 * Implementierungen: YamlStorage (Standard) und MySQLStorage.
 */
public interface IStorage {

    /** Initialisierung (Tabellen anlegen, Datei öffnen, etc.) */
    void init();

    /** Alle gespeicherten Spielerdaten laden */
    Collection<BankData> loadAll();

    /** Einzelnen Spieler speichern */
    void save(BankData data);

    /** Alle Spieler speichern */
    void saveAll(Collection<BankData> data);

    /** Spieler aus der Datenbank löschen */
    void remove(UUID uuid);

    /** Verbindung/Ressourcen schließen */
    void close();
}
