/*
**SQL File Conventions (as of hugsql "0.5.1")

****HugSQL recognizes the following keys:

    :name or :name- (private fn) = name of the function to create and,
    optionally, the command and result as a shorthand in place of providing
    these as separate key/value pairs

    :doc = docstring for the created function

    :command = underlying database command to run

    :result = expected result type

    :snip or :snip- (private fn) = name of the function to create and,
    optionally, the command and result as a shorthand in place of providing
    these as separate key/value pairs. :snip is used in place of :name for
    snippets.

    :meta metadata in the form of an EDN hashmap to attach to function

    :require = namespace require and aliases for Clojure expression support

****Command

  The :command specifies the underlying database command to run for the given
  SQL. The built-in values are:

    :query or :? = query with a result-set (default)

    :execute or :! = any statement

    :returning-execute or :<! = support for INSERT ... RETURNING

    :insert or :i! = support for insert and jdbc .getGeneratedKeys

    :query and :execute mirror the distinction between query and execute! in
    the clojure.java.jdbc library and fetch and execute in the clojure.jdbc
    library.

    :query is the default command when no command is specified.

****Result

  The :result specifies the expected result type for the given SQL.
  The available built-in values are:

    :one or :1 = one row as a hash-map

    :many or :* = many rows as a vector of hash-maps

    :affected or :n = number of rows affected (inserted/updated/deleted)

    :raw = passthrough an untouched result (default)

    :raw is the default when no result is specified.
*/

-- :name get-mining-results :? :*
-- :doc Get mining results. Volume and weight returned in tones.
SELECT
  kvdr_code AS worktype,
  DECODE(
    NVL(avweight, 0), 0, weightrate, avweight
  ) * tripnumbermanual AS weight,
  CASE
    WHEN
      weightrate = 0
      OR volumerate = 0
      THEN 0
    ELSE
    DECODE(
      NVL(avweight, 0), 0, weightrate, avweight
    ) * tripnumbermanual / (weightrate / volumerate)
  END AS volume
FROM
  dispatcher.shiftreportsadv
WHERE
  kvdr_code IN (:v*:worktypes)
  AND (
    (
      taskdate = TO_DATE(:report-date, 'dd.mm.yyyy') - 1
      AND shift = 5
      AND reportshift IN (1, 2)
    )
    OR (
      taskdate = TO_DATE(:report-date, 'dd.mm.yyyy')
      AND shift = 4
      AND reportshift = 4
    )
  )

-- :name get-ore-crushing-results :? :*
-- :doc Get ore crushing results. Weight returned in tones.
SELECT
  cwl.tag AS conveyor_tag,
  SUM(cwl.value) AS weight
FROM
  wonderwaregsm.conveyer_weigher_log cwl
WHERE
  cwl.type = '1H'
  AND cwl.tag IN (:v*:conveyor-tags)
  AND cwl.timestampend BETWEEN TO_DATE(
                                 :report-date || '00:50:00',
                                 'dd.mm.yyyy hh24:mi:ss'
                               ) 
                       AND TO_DATE(
                             :report-date || '00:10:00',
                             'dd.mm.yyyy hh24:mi:ss'
                           ) + 1
GROUP BY
  cwl.tag

-- :name get-conveyor-weigher-log :? :*
-- :doc Get conveyor weigher log. Weight returned in tones.
SELECT
  conv.ordering,
  conv.description,
  conv.department,
  conv.site,
  cwl.tag,
  cwl.date_begin,
  cwl.date_end,
  cwl.shift,
  cwl.hour_weight
FROM
  wonderwaregsm.conveyors conv
LEFT JOIN (
  SELECT
    tag,
    TO_CHAR(timestampbegin, 'dd.mm.yyyy hh24:mi:ss') AS date_begin,
    TO_CHAR(timestampend, 'dd.mm.yyyy hh24:mi:ss') AS date_end,
    value AS hour_weight,
    valuetotal AS total_weight,
    CASE
      WHEN timestampend BETWEEN TO_DATE(
                                  :request-date || '00:50:00',
                                  'dd.mm.yyyy hh24:mi:ss'
                                ) 
                            AND TO_DATE(
                                  :request-date || '08:10:00',
                                  'dd.mm.yyyy hh24:mi:ss'
                                )
      THEN 1
      WHEN timestampend BETWEEN TO_DATE(
                                  :request-date || '08:50:00',
                                  'dd.mm.yyyy hh24:mi:ss'
                                ) 
                            AND TO_DATE(
                                  :request-date || '16:10:00',
                                  'dd.mm.yyyy hh24:mi:ss'
                                )
      THEN 2
      WHEN timestampend BETWEEN TO_DATE(
                                  :request-date || '16:50:00',
                                  'dd.mm.yyyy hh24:mi:ss'
                                ) 
                            AND TO_DATE(
                                  :request-date || '00:10:00',
                                  'dd.mm.yyyy hh24:mi:ss'
                                ) + 1
      THEN 3
    END AS shift
  FROM
    wonderwaregsm.conveyer_weigher_log
  WHERE
    TYPE = '1H'
    AND tag IN (:v*:conveyor-tags)
    AND timestampend BETWEEN TO_DATE(
                                   :request-date || '00:50:00',
                                   'dd.mm.yyyy hh24:mi:ss'
                                 ) 
                         AND TO_DATE(
                               :request-date || '00:10:00',
                               'dd.mm.yyyy hh24:mi:ss'
                             ) + 1
) cwl
  ON conv.tag = cwl.tag
ORDER BY
  conv.ordering,
  cwl.date_end
