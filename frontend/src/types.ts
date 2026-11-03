export type SpaceType = 'CLASSROOM' | 'COMPUTER_LAB' | 'MEETING_ROOM' | 'AMPHITHEATER' | 'STUDY_ROOM' | 'OTHER'
export type SpaceStatus = 'ACTIVE' | 'MAINTENANCE'
export type Role = 'ADMIN' | 'STUDENT' | 'TEACHER'

export interface User {
  id: number
  courriel: string
  prenom: string
  nom: string
  role: Role
}

export interface Building {
  id: number
  nom: string
}

export interface Space {
  id: number
  code: string
  nom: string
  type: SpaceType
  capacite: number
  batiment: Building
  etage: number | null
  description: string | null
  statut: SpaceStatus
  archive: boolean
  reservable: boolean
}

export interface SpacePayload {
  code: string
  nom: string
  type: SpaceType
  capacite: number
  batimentId: number
  etage: number | null
  description: string | null
  statut: SpaceStatus
  archive?: boolean
}

export interface PageResult<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
