package org.vicnata.operations;

import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.GestorCSV;
import org.vicnata.red.ProtocolManager;
import org.vicnata.utils.IdGenerator;
import org.vicnata.validadores.validadorPaciente;

public class CreateHandler implements OperationHandler {

    @Override
    public String handle(String[] p) {
        // Esperado mínimo: CREATE \ NOMBRE \ APELLIDO \ DOC \ EDAD \ SEXO \ EMAIL \ NOTAS \ FASTA_ID \ SECUENCIA ...
        if (p.length < 10) return ProtocolManager.error("CREATE", "MISSING_FIELDS|min>=10 got=" + p.length);

        String nombre   = p[1];
        String apellido = p[2];
        String docId    = p[3];
        String edadStr  = p[4];
        String sexo     = p[5];
        String email    = p[6];
        String notas    = p[7];
        // String fastaId  = p[8]; // lo ignoramos para el ID oficial (lo genera el server)
        // String secuencia = p[9]; // se usará más adelante para FASTA/detecciones

        try {
            // 1) Validar datos de paciente
            int edad = validadorPaciente.validarBasicos(nombre, apellido, docId, edadStr, sexo, email);

            // 2) Cargar rutas desde configuration.properties
            GestorPropiedades cfg = new GestorPropiedades();
            String pathData        = cfg.getProperty("PATH_DATA");              // ej. ./data
            String pathCsvPacientes= cfg.getProperty("PATH_CSV_PACIENTES");     // ej. ./data/pacientes.csv

            // 3) Generar patient_id único
            String patientId = IdGenerator.nextPatientId(pathData);

            // 4) Armar DTO y guardar en CSV
            PacienteDTO pac = new PacienteDTO();
            pac.setPatientId(patientId);
            pac.setNombre(nombre);
            pac.setApellido(apellido);
            pac.setDocumentId(docId);
            pac.setEdad(edad);
            pac.setSexo(sexo.toUpperCase());
            pac.setContactEmail(email);
            pac.setClinicalNotes(notas);
            pac.setActive(true);

            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            repo.appendPaciente(pac);

            // 5) Responder OK con el patient_id oficial del servidor
            return ProtocolManager.ok("CREATE", "CREATED|" + patientId);

        } catch (IllegalArgumentException ex) {
            // Validación falló
            return ProtocolManager.error("CREATE", ex.getMessage());
        } catch (Exception ex) {
            // Cualquier otro error
            return ProtocolManager.error("CREATE", "INTERNAL_ERROR|" + ex.getMessage());
        }
    }
}