{{/*
=============================================================================
Named templates ("partials"). Defined once here, reused across the templates
with {{ include "bookmark-api.fullname" . }}.

Why bother: every resource needs a consistent name and label set. Doing it by
hand invites typos, and a typo in a selector silently breaks the Service.
=============================================================================
*/}}

{{/* The chart name, overridable. */}}
{{- define "bookmark-api.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Fully-qualified app name: "<release>-<chart>", e.g. "dev-bookmark-api".
Including the release name is what lets the same chart be installed twice in
one namespace without the two releases colliding.
Truncated to 63 chars — the Kubernetes DNS label limit.
*/}}
{{- define "bookmark-api.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "bookmark-api.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
The full label set, stamped on every resource. managed-by/version/chart are
metadata for humans and tooling.
*/}}
{{- define "bookmark-api.labels" -}}
helm.sh/chart: {{ include "bookmark-api.chart" . }}
{{ include "bookmark-api.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels — the subset used to match pods to their Deployment/Service.
These must NEVER include anything that changes between upgrades (like version):
a Deployment's selector is immutable, so a changing selector label breaks
`helm upgrade` outright.
*/}}
{{- define "bookmark-api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "bookmark-api.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "bookmark-api.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "bookmark-api.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
