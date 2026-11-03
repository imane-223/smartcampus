import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { SpaceFormPage } from './SpaceFormPage'
import type { Space } from '../types'

const mocks = vi.hoisted(() => ({ buildings: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), createBuilding: vi.fn() }))
vi.mock('../api/spaceService', () => ({ spaceService: mocks }))

const space: Space = { id: 4, code: 'SAL-A101', nom: 'Salle A101', type: 'CLASSROOM', capacite: 40,
  batiment: { id: 1, nom: 'Bâtiment A' }, etage: 1, description: null, statut: 'ACTIVE', archive: false, reservable: true }

function renderCreate() {
  return render(<MemoryRouter initialEntries={['/espaces/nouveau']}><Routes>
    <Route path="/espaces/nouveau" element={<SpaceFormPage/>}/><Route path="/espaces" element={<div>Catalogue</div>}/>
  </Routes></MemoryRouter>)
}

function renderEdit() {
  return render(<MemoryRouter initialEntries={['/espaces/4/modifier']}><Routes>
    <Route path="/espaces/:id/modifier" element={<SpaceFormPage/>}/><Route path="/espaces" element={<div>Catalogue</div>}/>
  </Routes></MemoryRouter>)
}

describe('SpaceFormPage', () => {
  beforeEach(() => {
    cleanup()
    vi.clearAllMocks()
    mocks.buildings.mockResolvedValue([{ id: 1, nom: 'Bâtiment A' }])
    mocks.create.mockResolvedValue(space)
    mocks.get.mockResolvedValue(space)
    mocks.update.mockResolvedValue(space)
  })

  it('crée un espace avec les valeurs validées et actualise le catalogue', async () => {
    renderCreate()
    fireEvent.change(await screen.findByLabelText(/code de l’espace/i), { target: { value: 'SAL-A101' } })
    fireEvent.change(screen.getByLabelText(/nom de l’espace/i), { target: { value: 'Salle A101' } })
    fireEvent.change(screen.getByLabelText(/capacité/i), { target: { value: '40' } })
    fireEvent.change(screen.getByLabelText('Bâtiment *'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: /créer l’espace/i }))

    await waitFor(() => expect(mocks.create).toHaveBeenCalledWith(expect.objectContaining({
      code: 'SAL-A101', nom: 'Salle A101', capacite: 40, batimentId: 1,
    })))
    expect(await screen.findByText('Catalogue')).toBeInTheDocument()
  })

  it('ajoute un bâtiment à la liste et le sélectionne', async () => {
    mocks.createBuilding.mockResolvedValue({ id: 2, nom: 'Bâtiment B' })
    renderCreate()
    fireEvent.click(await screen.findByRole('button', { name: /ajouter un bâtiment/i }))
    fireEvent.change(screen.getByRole('textbox', { name: /nom du nouveau bâtiment/i }), { target: { value: 'Bâtiment B' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter' }))
    await waitFor(() => expect(mocks.createBuilding).toHaveBeenCalledWith('Bâtiment B'))
    expect(screen.getByLabelText('Bâtiment *')).toHaveValue('2')
  })

  it('modifie un espace existant avec le contrat PUT complet', async () => {
    renderEdit()
    fireEvent.change(await screen.findByLabelText(/nom de l’espace/i), { target: { value: 'Salle A101 rénovée' } })
    fireEvent.click(screen.getByRole('button', { name: /enregistrer les modifications/i }))
    await waitFor(() => expect(mocks.update).toHaveBeenCalledWith(4, expect.objectContaining({
      code: 'SAL-A101', nom: 'Salle A101 rénovée', capacite: 40, batimentId: 1, archive: false,
    })))
    expect(await screen.findByText('Catalogue')).toBeInTheDocument()
  })
})
