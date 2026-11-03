package com.smartcampus.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.smartcampus.room.dto.EspaceCreateRequest;
import com.smartcampus.room.dto.EspaceUpdateRequest;
import com.smartcampus.room.exception.BatimentNotFoundException;
import com.smartcampus.room.exception.CodeEspaceConflictException;
import com.smartcampus.room.exception.EspaceNotFoundException;
import com.smartcampus.room.model.Batiment;
import com.smartcampus.room.model.Espace;
import com.smartcampus.room.model.StatutEspace;
import com.smartcampus.room.model.TypeEspace;
import com.smartcampus.room.repository.BatimentRepository;
import com.smartcampus.room.repository.EspaceRepository;

class EspaceServiceImplTest {

    private EspaceRepository espaceRepository;
    private BatimentRepository batimentRepository;
    private EspaceServiceImpl service;
    private Batiment batiment;

    @BeforeEach
    void setUp() {
        espaceRepository = mock(EspaceRepository.class);
        batimentRepository = mock(BatimentRepository.class);
        service = new EspaceServiceImpl(espaceRepository, batimentRepository);
        batiment = new Batiment("Bâtiment A");
        when(batimentRepository.findById(1L)).thenReturn(Optional.of(batiment));
        when(espaceRepository.saveAndFlush(any(Espace.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsValidSpaceAndNormalizesItsCode() {
        when(espaceRepository.existsByCodeIgnoreCase("SAL-A101")).thenReturn(false);

        var response = service.creer(create(" sal-a101 ", "Salle A101", 40, TypeEspace.CLASSROOM));

        assertThat(response.code()).isEqualTo("SAL-A101");
        assertThat(response.statut()).isEqualTo(StatutEspace.ACTIVE);
        assertThat(response.reservable()).isTrue();
    }

    @Test
    void rejectsDuplicateCodeWithoutCaseSensitivity() {
        when(espaceRepository.existsByCodeIgnoreCase("SAL-A101")).thenReturn(true);

        assertThatThrownBy(() -> service.creer(create("sal-a101", "Salle A101", 40, TypeEspace.CLASSROOM)))
                .isInstanceOf(CodeEspaceConflictException.class);
    }

    @Test
    void rejectsUnknownBuilding() {
        when(espaceRepository.existsByCodeIgnoreCase("SAL-A101")).thenReturn(false);
        when(batimentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(new EspaceCreateRequest("SAL-A101", "Salle", TypeEspace.CLASSROOM,
                40, 9L, null, null, null))).isInstanceOf(BatimentNotFoundException.class);
    }

    @Test
    void updatesAnExistingSpaceWithoutConflictingWithItsOwnCode() {
        Espace espace = new Espace("SAL-A101", "Salle A101", TypeEspace.CLASSROOM, 40, batiment,
                1, null, StatutEspace.ACTIVE, false);
        when(espaceRepository.findById(4L)).thenReturn(Optional.of(espace));
        when(espaceRepository.existsByCodeIgnoreCaseAndIdNot("SAL-A101", 4L)).thenReturn(false);

        var response = service.modifier(4L, update("SAL-A101", "Salle A101 rénovée", false));

        assertThat(response.nom()).isEqualTo("Salle A101 rénovée");
        verify(espaceRepository).existsByCodeIgnoreCaseAndIdNot("SAL-A101", 4L);
    }

    @Test
    void rejectsModificationToAnotherSpacesCode() {
        when(espaceRepository.findById(4L)).thenReturn(Optional.of(
                new Espace("SAL-A101", "Salle A101", TypeEspace.CLASSROOM, 40, batiment,
                        1, null, StatutEspace.ACTIVE, false)));
        when(espaceRepository.existsByCodeIgnoreCaseAndIdNot("SAL-A102", 4L)).thenReturn(true);

        assertThatThrownBy(() -> service.modifier(4L, update("SAL-A102", "Salle A101", false)))
                .isInstanceOf(CodeEspaceConflictException.class);
    }

    @Test
    void rejectsModificationOfMissingSpace() {
        when(espaceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(404L, update("SAL-A101", "Salle", false)))
                .isInstanceOf(EspaceNotFoundException.class);
    }

    @Test
    void maintenanceSpacesRemainVisibleButAreNotReservable() {
        Espace maintenance = new Espace("LAB-01", "Laboratoire", TypeEspace.COMPUTER_LAB, 20,
                batiment, 0, null, StatutEspace.MAINTENANCE, false);
        when(espaceRepository.findById(5L)).thenReturn(Optional.of(maintenance));

        assertThat(service.statutDe(5L)).isEqualTo(StatutEspace.MAINTENANCE);
        assertThat(service.estReservable(5L)).isFalse();
    }

    @Test
    void archivedSpaceStillExistsButIsNotReservable() {
        Espace archived = new Espace("SAL-OLD", "Ancienne salle", TypeEspace.CLASSROOM, 10,
                batiment, 0, null, StatutEspace.ACTIVE, true);
        when(espaceRepository.existsById(8L)).thenReturn(true);
        when(espaceRepository.findById(8L)).thenReturn(Optional.of(archived));

        assertThat(service.existe(8L)).isTrue();
        assertThat(service.estArchive(8L)).isTrue();
        assertThat(service.estReservable(8L)).isFalse();
    }

    @Test
    void usesRepositoryFilteringForTheCatalog() {
        when(espaceRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        var result = service.rechercher(TypeEspace.CLASSROOM, 1L, StatutEspace.ACTIVE,
                "A101", PageRequest.of(0, 20));

        assertThat(result).isEmpty();
        verify(espaceRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }

    private static EspaceCreateRequest create(String code, String nom, int capacite, TypeEspace type) {
        return new EspaceCreateRequest(code, nom, type, capacite, 1L, 1, "Description", null);
    }

    private static EspaceUpdateRequest update(String code, String nom, boolean archived) {
        return new EspaceUpdateRequest(code, nom, TypeEspace.CLASSROOM, 40,
                1L, 1, null, StatutEspace.ACTIVE, archived);
    }
}
