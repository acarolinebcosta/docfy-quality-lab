import { profile } from "../config/profiles.js";
import {
  browseDocumentCatalog,
  prepareDocumentCatalog,
} from "../lib/document-catalog-journey.js";
import { performanceSummary } from "../lib/evidence.js";

export const options = profile("load").options;

export const setup = prepareDocumentCatalog;

export default browseDocumentCatalog;

export function handleSummary(data) {
  return performanceSummary(data, "load");
}
