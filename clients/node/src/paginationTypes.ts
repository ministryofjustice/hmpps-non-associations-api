/**
 * A page of items returned from a long list.
 *
 * Simplified version of Spring’s
 * org.springframework.data.domain.Page
 * ignoring repeated and unnecessary fields
 */
export interface Page<Item = unknown> {
  /** Items in the page */
  content: Item[]
  /** Page number, indexed from 0 */
  number: number
  /** Number of pages */
  totalPages: number
  /** Total number of items across all pages */
  totalElements: number
}

/**
 * Request to get a page of items from a long list.
 *
 * Simplified version of Spring’s
 * org.springframework.data.domain.PageRequest
 * ignoring unnecessary fields and with generic type for sorting
 */
export interface PageRequest<SortField extends string = never> {
  /** Page number, indexed from 0 and defaulting to 0 */
  page?: number
  /** Items per page, defaulting to 20, with 200 as the maximum */
  size?: number
  /** Sorting options */
  sort?: SortField | `${SortField},ASC` | `${SortField},DESC`
}
